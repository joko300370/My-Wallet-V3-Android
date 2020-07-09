package piuk.blockchain.android.coincore.fiat

import info.blockchain.balance.ExchangeRates
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.balance.total
import io.reactivex.Single
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SendProcessor
import piuk.blockchain.android.coincore.SendState
import piuk.blockchain.android.coincore.SingleAccountList

internal class FiatCustodialAccount(
    override val label: String,
    override val fiatCurrency: String,
    override val isDefault: Boolean = false,
    // STUBS for testing
    private val funds: FiatValue
) : FiatAccount {

    override val balance: Single<Money>
        get() = Single.just(funds)

    override val activity: Single<ActivitySummaryList>
        get() = Single.just(emptyList())

    override val actions: AvailableActions
        get() = setOf(AssetAction.ViewActivity)

    override val isFunded: Boolean
        get() = !funds.isZero

    override val hasTransactions: Boolean
        get() = false

    override fun fiatBalance(fiatCurrency: String, exchangeRates: ExchangeRates): Single<Money> =
        balance.map { it.toFiat(exchangeRates, fiatCurrency) }

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.error(NotImplementedError("Send to fiat not supported"))

    override val sendState: Single<SendState>
        get() = Single.just(SendState.NOT_SUPPORTED)

    override fun createSendProcessor(address: ReceiveAddress): Single<SendProcessor> =
        Single.error(NotImplementedError("Send to fiat not supported"))
}

class FiatAccountGroup(
    override val label: String,
    override val accounts: SingleAccountList
) : AccountGroup {
    // Produce the sum of all balances of all accounts
    override val balance: Single<Money>
        get() = Single.error(NotImplementedError("No unified balance for All Fiat accounts"))

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
            Single.zip(
                accounts.map { it.fiatBalance(fiatCurrency, exchangeRates) }
            ) { t: Array<Any> ->
                t.map { it as Money }
                    .total()
            }
        }

    override fun includes(account: BlockchainAccount): Boolean =
        accounts.contains(account)
}