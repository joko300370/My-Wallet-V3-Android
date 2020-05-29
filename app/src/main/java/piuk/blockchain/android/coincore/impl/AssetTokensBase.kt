package piuk.blockchain.android.coincore.impl

import com.blockchain.logging.CrashLogger

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.AssetTokens
import piuk.blockchain.android.coincore.CryptoAccountGroup
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.CryptoSingleAccountList
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.extensions.then
import timber.log.Timber

internal abstract class AssetTokensBase(
    protected val exchangeRates: ExchangeRateDataManager,
    private val historicRates: ChartsDataManager,
    protected val currencyPrefs: CurrencyPrefs,
    private val labels: DefaultLabels,
    private val custodialManager: CustodialWalletManager,
    private val pitLinking: PitLinking,
    protected val crashLogger: CrashLogger,
    rxBus: RxBus
) : AssetTokens {

    val logoutSignal = rxBus.register(AuthEvent.UNPAIR::class.java)
        .observeOn(Schedulers.computation())
        .subscribeBy(onNext = ::onLogoutSignal)

    private val accounts = mutableListOf<CryptoSingleAccount>()

    // Init token, set up accounts and fetch a few activities
    fun init(): Completable =
        initToken()
            .doOnError { throwable ->
                crashLogger.logException(throwable, "Coincore: Failed to load $asset wallet")
            }
            .then { loadAccounts() }
            .doOnComplete { Timber.d("Coincore: Init $asset Complete") }
            .doOnError { Timber.d("Coincore: Init $asset Failed") }

    private fun loadAccounts(): Completable =
        Completable.fromCallable { accounts.clear() }
            .then {
                loadNonCustodialAccounts(labels)
                    .doOnSuccess { accounts.addAll(it) }
                    .ignoreElement()
            }
            .then {
                loadCustodialAccount()
                    .doOnSuccess { accounts.addAll(it) }
                    .ignoreElement()
            }
            .then {
                loadInterestAccounts(labels)
                    .doOnSuccess { accounts.addAll(it) }
                    .ignoreElement()
            }
            .doOnError { Timber.e("Error loading accounts for ${asset.networkTicker}: $it") }

    abstract fun initToken(): Completable

    abstract fun loadNonCustodialAccounts(labels: DefaultLabels): Single<CryptoSingleAccountList>

    private fun loadInterestAccounts(labels: DefaultLabels): Single<CryptoSingleAccountList> =
        Single.fromCallable {
            listOf(
                CryptoInterestAccount(
                    asset,
                    labels.getDefaultInterestWalletLabel(asset),
                    custodialManager,
                    exchangeRates
                )
            )
        }

    override fun interestRate(): Single<Double> = custodialManager.getInterestAccountRates(asset)

    private fun loadCustodialAccount(): Single<CryptoSingleAccountList> =
        Single.fromCallable {
            listOf(
                CustodialTradingAccount(
                    asset,
                    labels.getDefaultCustodialWalletLabel(asset),
                    exchangeRates,
                    custodialManager
                )
            )
        }

    protected open fun onLogoutSignal(event: AuthEvent) {}

    final override fun accounts(filter: AssetFilter): Single<CryptoAccountGroup> =
        Single.fromCallable {
            filterTokenAccounts(asset, labels, accounts, filter)
        }

    final override fun defaultAccount(): Single<CryptoSingleAccount> =
        Single.fromCallable {
            accounts.first { it.isDefault }
        }

    final override fun exchangeRate(): Single<FiatValue> =
        exchangeRates.fetchLastPrice(asset, currencyPrefs.selectedFiatCurrency)

    final override fun historicRate(epochWhen: Long): Single<FiatValue> =
        exchangeRates.getHistoricPrice(asset, currencyPrefs.selectedFiatCurrency, epochWhen)

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        historicRates.getHistoricPriceSeries(asset, currencyPrefs.selectedFiatCurrency, period)

    protected fun getPitLinkingAddress(): Maybe<ReceiveAddress> =
        pitLinking.isPitLinked().filter { it }
            .flatMap { custodialManager.getExchangeSendAddressFor(asset) }
            .map { address ->
                ExchangeAddress(asset, address, labels)
            }

    final override fun validateAddress(address: String): CryptoAddress? =
        if(isValidAddress(address)) {
            EnteredAddress(asset, address)
        } else {
            null
        }

    // These are constant ATM, but may need to change this so hardcode here
    protected val transactionFetchCount = 50
    protected val transactionFetchOffset = 0
}

fun ExchangeRateDataManager.fetchLastPrice(
    cryptoCurrency: CryptoCurrency,
    currencyName: String
): Single<FiatValue> =
    updateTickers()
        .andThen(Single.defer { Single.just(getLastPrice(cryptoCurrency, currencyName)) })
        .map { FiatValue.fromMajor(currencyName, it.toBigDecimal(), false) }
