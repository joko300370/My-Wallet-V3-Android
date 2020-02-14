package piuk.blockchain.android.coincore

import info.blockchain.api.data.Transaction
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.extensions.switchToSingleIfEmpty
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

enum class AssetFilter {
    Total,
    Wallet,
//    ColdStorage,
//    Lockbox,
    Custodial
}

enum class AssetAction {
    ViewActivity,
    Send,
    Receive,
    Swap
}

typealias AvailableActions = Set<AssetAction>

typealias TransactionList = List<Transaction>

// TODO: For account fetching/default accounts look steal the code from xxxAccountListAdapter in core

interface AssetTokens {
    val asset: CryptoCurrency

    fun defaultAccount(): Single<AccountReference>
//    fun accounts(): Single<AccountsList>

    fun totalBalance(filter: AssetFilter = AssetFilter.Total): Single<CryptoValue>
    fun balance(account: AccountReference): Single<CryptoValue>

    fun exchangeRate(): Single<FiatValue>
    fun historicRate(epochWhen: Long): Single<FiatValue>
    fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries>

//    fun transactions(): Single<TransactionList>
//    fun transactions(account: AccountReference): Single<TransactionList>

//    interface PendingTransaction { }
//    fun computeFees(priority: FeePriority, pending: PendingTransaction): Single<PendingTransaction>
//    fun validate(pending: PendingTransaction): Boolean
//    fun execute(pending: PendingTransaction)

    fun actions(filter: AssetFilter): AvailableActions

    /** Has this user got a configured wallet for asset type?
    // The balance methods will return zero for un-configured wallets - ie custodial - but we need a way
    // to differentiate between zero and not configured, so call this in the dashboard asset view when
    // deciding if to show custodial etc **/
    fun hasActiveWallet(filter: AssetFilter): Boolean
}

abstract class AssetTokensBase(rxBus: RxBus) : AssetTokens {

    val logoutSignal = rxBus.register(AuthEvent.UNPAIR::class.java)
        .observeOn(Schedulers.computation())
        .subscribeBy(onNext = ::onLogoutSignal)

    protected open fun onLogoutSignal(event: AuthEvent) { }

    final override fun totalBalance(filter: AssetFilter): Single<CryptoValue> =
        when (filter) {
            AssetFilter.Wallet -> noncustodialBalance()
            AssetFilter.Custodial -> custodialBalance()
            AssetFilter.Total -> Singles.zip(
                noncustodialBalance(),
                custodialBalance()
            ) { noncustodial, custodial -> noncustodial + custodial }
        }

    protected abstract fun custodialBalanceMaybe(): Maybe<CryptoValue>
    protected abstract fun noncustodialBalance(): Single<CryptoValue>

    private val isNonCustodialConfigured = AtomicBoolean(false)

    private fun custodialBalance(): Single<CryptoValue> =
        custodialBalanceMaybe()
            .doOnComplete { isNonCustodialConfigured.set(false) }
            .doOnSuccess { isNonCustodialConfigured.set(true) }
            .switchToSingleIfEmpty { Single.just(CryptoValue.zero(asset)) }
            // Report and then eat errors getting custodial balances - TODO add UI element to inform the user?
            .onErrorReturn {
                Timber.d("Unable to get non-custodial balance: $it")
                CryptoValue.zero(asset)
            }

    protected open val noncustodialActions = setOf(
        AssetAction.ViewActivity,
        AssetAction.Send,
        AssetAction.Receive,
        AssetAction.Swap
    )

    protected open val custodialActions = setOf(
        AssetAction.Send
    )

    override fun actions(filter: AssetFilter): AvailableActions =
        when (filter) {
            AssetFilter.Total -> custodialActions.intersect(noncustodialActions)
            AssetFilter.Custodial -> custodialActions
            AssetFilter.Wallet -> noncustodialActions
        }

    override fun hasActiveWallet(filter: AssetFilter): Boolean =
        when (filter) {
            AssetFilter.Total -> true
            AssetFilter.Wallet -> true
            AssetFilter.Custodial -> isNonCustodialConfigured.get()
        }
}

fun ExchangeRateDataManager.fetchLastPrice(
    cryptoCurrency: CryptoCurrency,
    currencyName: String
): Single<FiatValue> =
    updateTickers()
        .andThen(Single.defer { Single.just(getLastPrice(cryptoCurrency, currencyName)) })
        .map { FiatValue.fromMajor(currencyName, it.toBigDecimal(), false) }
