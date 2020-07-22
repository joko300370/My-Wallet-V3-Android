package piuk.blockchain.android.coincore.impl

import com.blockchain.swap.nabu.datamanagers.BuyOrder
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.OrderState
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRates
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.balance.total
import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.CustodialActivitySummaryItem
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SendProcessor
import piuk.blockchain.android.coincore.SendState
import piuk.blockchain.android.coincore.SendTarget
import piuk.blockchain.android.coincore.TransferError
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.utils.extensions.mapList
import piuk.blockchain.androidcore.utils.extensions.switchToSingleIfEmpty
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

internal const val transactionFetchCount = 50
internal const val transactionFetchOffset = 0

abstract class CryptoAccountBase(
    final override val asset: CryptoCurrency
) : CryptoAccount {

    protected abstract val exchangeRates: ExchangeRateDataManager

    final override var hasTransactions: Boolean = false
        private set

    final override fun fiatBalance(
        fiatCurrency: String,
        exchangeRates: ExchangeRates
    ): Single<Money> =
        balance.map { it.toFiat(exchangeRates, fiatCurrency) }

    protected fun setHasTransactions(hasTransactions: Boolean) {
        this.hasTransactions = hasTransactions
    }

    override val sendState: Single<SendState>
        get() = Single.just(SendState.NOT_SUPPORTED)
}

open class CustodialTradingAccount(
    cryptoCurrency: CryptoCurrency,
    override val label: String,
    override val exchangeRates: ExchangeRateDataManager,
    val custodialWalletManager: CustodialWalletManager
) : CryptoAccountBase(cryptoCurrency) {

    private val hasSeenFunds = AtomicBoolean(false)

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.error(NotImplementedError("Custodial accounts don't support receive"))

    override val balance: Single<Money>
        get() = custodialWalletManager.getBalanceForAsset(asset)
            .doOnComplete { hasSeenFunds.set(false) }
            .doOnSuccess { hasSeenFunds.set(true) }
            .switchToSingleIfEmpty { Single.just(CryptoValue.zero(asset)) }
            .onErrorReturn {
                Timber.d("Unable to get custodial trading balance: $it")
                CryptoValue.zero(asset)
            }
            .map { it as Money }

    override val activity: Single<ActivitySummaryList>
        get() = custodialWalletManager.getAllBuyOrdersFor(asset)
            .mapList { buyOrderToSummary(it) }
            .filterActivityStates()
            .doOnSuccess { setHasTransactions(it.isNotEmpty()) }
            .onErrorReturn { emptyList() }

    override val isFunded: Boolean
        get() = hasSeenFunds.get()

    override val isDefault: Boolean =
        false // Default is, presently, only ever a non-custodial account.

    override fun createSendProcessor(sendTo: SendTarget): Single<SendProcessor> =
        when (sendTo) {
            is CryptoAddress -> Single.just(
                CustodialTransferProcessor(
                    sendingAccount = this,
                    address = sendTo,
                    walletManager = custodialWalletManager
                )
            )
            is CryptoAccount -> sendTo.receiveAddress.map {
                CustodialTransferProcessor(
                    sendingAccount = this,
                    address = it as CryptoAddress,
                    walletManager = custodialWalletManager
                )
            }
            else -> Single.error(TransferError("Cannot send custodial crypto to a non-crypto target"))
        }

    override val sendState: Single<SendState>
        get() = balance.map { balance ->
                if (balance <= CryptoValue.zero(asset))
                    SendState.NO_FUNDS
                else
                    SendState.CAN_SEND
            }

    override val actions: AvailableActions
        get() = availableActions

    private val availableActions = setOf(
        AssetAction.ViewActivity,
        AssetAction.NewSend
    )

    private fun buyOrderToSummary(buyOrder: BuyOrder): ActivitySummaryItem =
        CustodialActivitySummaryItem(
            exchangeRates = exchangeRates,
            cryptoCurrency = buyOrder.crypto.currency,
            value = buyOrder.crypto,
            fundedFiat = buyOrder.fiat,
            txId = buyOrder.id,
            timeStampMs = buyOrder.created.time,
            status = buyOrder.state,
            fee = buyOrder.fee ?: FiatValue.zero(buyOrder.fiat.currencyCode),
            account = this,
            paymentMethodId = buyOrder.paymentMethodId
        )

    // Stop gap filter, until we finalise which item we wish to display to the user.
    // TODO: This can be done via the API when it's settled
    private fun Single<ActivitySummaryList>.filterActivityStates(): Single<ActivitySummaryList> {
        return flattenAsObservable { list ->
            list.filter {
                it is CustodialActivitySummaryItem && displayedStates.contains(it.status)
            }
        }.toList()
    }

    companion object {
        private val displayedStates = setOf(
            OrderState.FINISHED,
            OrderState.AWAITING_FUNDS,
            OrderState.PENDING_EXECUTION
        )
    }
}

internal class CryptoInterestAccount(
    cryptoCurrency: CryptoCurrency,
    override val label: String,
    val custodialWalletManager: CustodialWalletManager,
    override val exchangeRates: ExchangeRateDataManager
) : CryptoAccountBase(cryptoCurrency) {

    private val isConfigured = AtomicBoolean(false)

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.error(NotImplementedError("Interest accounts don't support receive"))

    override val balance: Single<Money>
        get() = custodialWalletManager.getInterestAccountDetails(asset)
            .doOnSuccess {
                isConfigured.set(true)
            }.doOnComplete {
                isConfigured.set(false)
            }.switchIfEmpty(
                Single.just(CryptoValue.zero(asset))
            )
            .map { it as Money }

    override val activity: Single<ActivitySummaryList>
        get() = Single.just(emptyList())

    override val isFunded: Boolean
        get() = isConfigured.get()

    override val isDefault: Boolean =
        false // Default is, presently, only ever a non-custodial account.

    override fun createSendProcessor(sendTo: SendTarget): Single<SendProcessor> =
        Single.error<SendProcessor>(NotImplementedError("Cannot Send from Interest Wallet"))

    override val sendState: Single<SendState>
        get() = Single.just(SendState.NOT_SUPPORTED)

    override val actions: AvailableActions
        get() = availableActions

    private val availableActions = emptySet<AssetAction>()
}

// To handle Send to PIT
internal class CryptoExchangeAccount(
    cryptoCurrency: CryptoCurrency,
    override val label: String,
    private val address: String,
    override val exchangeRates: ExchangeRateDataManager
) : CryptoAccountBase(cryptoCurrency) {

    override val balance: Single<Money>
        get() = Single.just(CryptoValue.zero(asset))

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(
            ExchangeAddress(
                asset = asset,
                label = label,
                address = address
            )
        )

    override val isDefault: Boolean = false
    override val isFunded: Boolean = false

    override fun createSendProcessor(sendTo: SendTarget): Single<SendProcessor> =
        Single.error<SendProcessor>(NotImplementedError("Cannot Send from Exchange Wallet"))

    override val activity: Single<ActivitySummaryList>
        get() = Single.just(emptyList())

    override val actions: AvailableActions = emptySet()
}

abstract class CryptoNonCustodialAccount(
    cryptoCurrency: CryptoCurrency
) : CryptoAccountBase(cryptoCurrency) {

    override val isFunded: Boolean = true

    override val actions: AvailableActions = setOf(
        AssetAction.ViewActivity,
        AssetAction.Send,
        AssetAction.Receive,
        AssetAction.Swap
    )

    override fun createSendProcessor(sendTo: SendTarget): Single<SendProcessor> {
        TODO("Implement me")
    }
}

// Currently only one custodial account is supported for each asset,
// so all the methods on this can just delegate directly
// to the (required) CryptoSingleAccountCustodialBase

class CryptoAccountCustodialGroup(
    override val label: String,
    override val accounts: SingleAccountList
) : AccountGroup {

    private val account: CryptoAccountBase

    init {
        require(accounts.size == 1)
        require(accounts[0] is CryptoInterestAccount || accounts[0] is CustodialTradingAccount)
        account = accounts[0] as CryptoAccountBase
    }

    override val balance: Single<Money>
        get() = account.balance

    override val activity: Single<ActivitySummaryList>
        get() = account.activity

    override val actions: AvailableActions
        get() = account.actions

    override val isFunded: Boolean
        get() = account.isFunded

    override val hasTransactions: Boolean
        get() = account.hasTransactions

    override fun fiatBalance(
        fiatCurrency: String,
        exchangeRates: ExchangeRates
    ): Single<Money> =
        balance.map { it.toFiat(exchangeRates, fiatCurrency) }

    override fun includes(account: BlockchainAccount): Boolean =
        accounts.contains(account)
}

class CryptoAccountNonCustodialGroup(
    val asset: CryptoCurrency,
    override val label: String,
    override val accounts: SingleAccountList
) : AccountGroup {
    // Produce the sum of all balances of all accounts
    override val balance: Single<Money>
        get() = if (accounts.isEmpty()) {
            Single.just(CryptoValue.zero(asset))
        } else {
            Single.zip(
                accounts.map { it.balance }
            ) { t: Array<Any> ->
                t.map { it as Money }
                    .total()
            }
        }

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
    override val actions: AvailableActions
        get() = if (accounts.isEmpty()) {
            emptySet()
        } else {
            accounts.map { it.actions }.reduce { a, b -> a.intersect(b) }
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
            balance.map { it.toFiat(exchangeRates, fiatCurrency) }
        }

    override fun includes(account: BlockchainAccount): Boolean =
        accounts.contains(account)
}
