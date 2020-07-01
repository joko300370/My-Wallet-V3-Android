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
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.AssetTokens
import piuk.blockchain.android.coincore.CryptoAccountGroup
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.CryptoSingleAccountList
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.utils.extensions.then
import timber.log.Timber

internal abstract class AssetTokensBase(
    protected val exchangeRates: ExchangeRateDataManager,
    private val historicRates: ChartsDataManager,
    protected val currencyPrefs: CurrencyPrefs,
    protected val labels: DefaultLabels,
    protected val custodialManager: CustodialWalletManager,
    private val pitLinking: PitLinking,
    protected val crashLogger: CrashLogger
) : AssetTokens {

    private val accounts = mutableListOf<CryptoSingleAccount>()

    // Init token, set up accounts and fetch a few activities
    override fun init(): Completable =
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

    open fun loadCustodialAccount(): Single<CryptoSingleAccountList> =
        Single.just(
            listOf(CustodialTradingAccount(
                asset,
                labels.getDefaultCustodialWalletLabel(asset),
                exchangeRates,
                custodialManager
            ))
        )

    final override fun accounts(filter: AssetFilter): Single<CryptoAccountGroup> =
        Single.fromCallable {
            filterTokenAccounts(asset, labels, accounts, filter)
        }

    final override fun defaultAccount(): Single<CryptoSingleAccount> =
        Single.fromCallable {
            accounts.first { it.isDefault }
        }

    private fun getNonCustodialAccountList(): Single<CryptoSingleAccountList> =
        accounts(filter = AssetFilter.Wallet)
            .doOnSuccess { Timber.d("@@@@ got unfiltered list: $it") }
            .map { group -> group.accounts.mapNotNull { it as? CryptoSingleAccount } }
            .doOnSuccess { Timber.d("@@@@ got list: $it") }

    final override fun exchangeRate(): Single<FiatValue> =
        exchangeRates.fetchLastPrice(asset, currencyPrefs.selectedFiatCurrency)

    final override fun historicRate(epochWhen: Long): Single<FiatValue> =
        exchangeRates.getHistoricPrice(asset, currencyPrefs.selectedFiatCurrency, epochWhen)

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        historicRates.getHistoricPriceSeries(asset, currencyPrefs.selectedFiatCurrency, period)

    protected fun getPitLinkingAccount(): Maybe<CryptoSingleAccount> =
        pitLinking.isPitLinked().filter { it }
            .flatMap { custodialManager.getExchangeSendAddressFor(asset) }
            .map { address ->
                CryptoExchangeAccount(
                    cryptoCurrency = asset,
                    label = labels.getDefaultExchangeWalletLabel(asset),
                    address = address,
                    exchangeRates = exchangeRates
                )
            }

    final override fun canTransferTo(account: CryptoSingleAccount): Single<CryptoSingleAccountList> {
        require(account.cryptoCurrencies.contains(asset))

        return when (account) {
            is CustodialTradingAccount -> getNonCustodialAccountList()
            is CryptoInterestAccount -> Single.just(emptyList())
            is CryptoExchangeAccount -> Single.just(emptyList())
            is CryptoNonCustodialAccount -> getPitLinkingAccount()
                    .map { listOf(it) }
                    .toSingle(emptyList())
            else -> Single.just(emptyList())
        }
    }
}

fun ExchangeRateDataManager.fetchLastPrice(
    cryptoCurrency: CryptoCurrency,
    currencyName: String
): Single<FiatValue> =
    updateTickers()
        .andThen(Single.defer { Single.just(getLastPrice(cryptoCurrency, currencyName)) })
        .map { FiatValue.fromMajor(currencyName, it.toBigDecimal(), false) }
