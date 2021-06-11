package piuk.blockchain.android.coincore.fiat

import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.TransactionState
import com.blockchain.nabu.datamanagers.TransactionType
import com.blockchain.nabu.datamanagers.repositories.CustodialAssetWalletsBalancesRepository
import com.blockchain.nabu.models.responses.interest.DisabledReason
import info.blockchain.balance.ExchangeRates
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.balance.total
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.FiatActivitySummaryItem
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.TxSourceState
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import java.util.concurrent.atomic.AtomicBoolean

internal class FiatCustodialAccount(
    override val label: String,
    override val fiatCurrency: String,
    override val isDefault: Boolean = false,
    private val custodialAssetWalletsBalancesRepository: CustodialAssetWalletsBalancesRepository,
    private val custodialWalletManager: CustodialWalletManager,
    private val exchangesRatesDataManager: ExchangeRateDataManager
) : FiatAccount {
    private val hasFunds = AtomicBoolean(false)

    override val accountBalance: Single<Money>
        get() = custodialAssetWalletsBalancesRepository.getFiatTotalBalanceForAsset(fiatCurrency)
            .toSingle(FiatValue.zero(fiatCurrency))
            .map {
                it as Money
            }.doOnSuccess {
                hasFunds.set(it.isPositive)
            }

    override val actionableBalance: Single<Money>
        get() = custodialAssetWalletsBalancesRepository.getFiatActionableBalanceForAsset(fiatCurrency)
            .toSingle(FiatValue.zero(fiatCurrency))
            .map {
                it as Money
            }.doOnSuccess {
                hasFunds.set(it.isPositive)
            }

    override val pendingBalance: Single<Money>
        get() = custodialAssetWalletsBalancesRepository.getFiatPendingBalanceForAsset(fiatCurrency)
            .toSingle(FiatValue.zero(fiatCurrency))
            .map {
                it as Money
            }

    override var hasTransactions: Boolean = false
        private set

    override val activity: Single<ActivitySummaryList>
        get() = custodialWalletManager.getCustodialFiatTransactions(fiatCurrency, Product.BUY)
            .doOnSuccess {
                setHasTransactions(it.isEmpty().not())
            }.map {
                it.map { fiatTransaction ->
                    FiatActivitySummaryItem(
                        currency = fiatCurrency,
                        exchangeRates = exchangesRatesDataManager,
                        txId = fiatTransaction.id,
                        timeStampMs = fiatTransaction.date.time,
                        value = fiatTransaction.amount,
                        account = this,
                        state = fiatTransaction.state,
                        type = fiatTransaction.type
                    )
                }
            }

    override fun canWithdrawFunds(): Single<Boolean> =
        custodialWalletManager.getCustodialFiatTransactions(fiatCurrency, Product.BUY).map {
            it.filter { tx -> tx.type == TransactionType.WITHDRAWAL && tx.state == TransactionState.PENDING }
        }.map {
            it.isEmpty()
        }

    override val actions: Single<AvailableActions> =
        custodialWalletManager.canTransactWithBankMethods(fiatCurrency)
            .zipWith(actionableBalance.map { it.isPositive })
            .map { (canTransactWithBanks, hasActionableBalance) ->
                if (canTransactWithBanks) {
                    setOfNotNull(
                        AssetAction.ViewActivity,
                        AssetAction.FiatDeposit,
                        if (hasActionableBalance) AssetAction.Withdraw else null
                    )
                } else {
                    setOf(AssetAction.ViewActivity)
                }
            }

    override val isFunded: Boolean
        get() = hasFunds.get()

    private fun setHasTransactions(hasTransactions: Boolean) {
        this.hasTransactions = hasTransactions
    }

    override fun fiatBalance(fiatCurrency: String, exchangeRates: ExchangeRates): Single<Money> =
        accountBalance.map { it.toFiat(exchangeRates, fiatCurrency) }

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.error(NotImplementedError("Send to fiat not supported"))

    override val sourceState: Single<TxSourceState>
        get() = Single.just(TxSourceState.NOT_SUPPORTED)

    override val isEnabled: Single<Boolean>
        get() = Single.just(true)

    override val disabledReason: Single<DisabledReason>
        get() = Single.just(DisabledReason.NONE)
}

class FiatAccountGroup(
    override val label: String,
    override val accounts: SingleAccountList
) : AccountGroup {
    // Produce the sum of all balances of all accounts
    override val accountBalance: Single<Money>
        get() = Single.error(NotImplementedError("No unified balance for All Fiat accounts"))

    override val actionableBalance: Single<Money>
        get() = Single.error(NotImplementedError("No unified balance for All Fiat accounts"))

    override val pendingBalance: Single<Money>
        get() = Single.error(NotImplementedError("No unified pending balance for All Fiat accounts"))

    // All the activities for all the accounts
    override val activity: Single<ActivitySummaryList>
        get() = if (accounts.isEmpty()) {
            Single.just(emptyList())
        } else {
            Single.zip(
                accounts.map { it.activity }
            ) { t: Array<Any> ->
                t.filterIsInstance<List<ActivitySummaryItem>>().flatten()
            }
        }

    // The intersection of the actions for each account
    override val actions: Single<AvailableActions>
        get() = if (accounts.isEmpty()) {
            Single.just(emptySet())
        } else {
            Single.zip(
                accounts.map { it.actions }
            ) { t: Array<Any> ->
                t.filterIsInstance<AvailableActions>().flatten().toSet()
            }
        }

    // if _any_ of the accounts have transactions
    override val hasTransactions: Boolean
        get() = accounts.map { it.hasTransactions }.any { it }

    // Are any of the accounts funded
    override val isFunded: Boolean = accounts.map { it.isFunded }.any { it }

    override fun fiatBalance(
        fiatCurrency: String,
        exchangeRates: ExchangeRates
    ): Single<Money> =
        if (accounts.isEmpty()) {
            Single.just(FiatValue.zero(fiatCurrency))
        } else {
            Single.zip(
                accounts.map { it.fiatBalance(fiatCurrency, exchangeRates) }
            ) { t: Array<Any> ->
                t.map { it as Money }
                    .total()
            }
        }

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.error(NotImplementedError("No receive addresses for All Fiat accounts"))

    override fun includes(account: BlockchainAccount): Boolean =
        accounts.contains(account)
}