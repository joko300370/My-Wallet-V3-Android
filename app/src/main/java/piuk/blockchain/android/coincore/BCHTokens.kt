package piuk.blockchain.android.coincore

import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.toAccountReference
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import timber.log.Timber

class BCHTokens(
    private val bchDataManager: BchDataManager,
    private val exchangeRates: ExchangeRateDataManager,
    private val historicRates: ChartsDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val stringUtils: StringUtils,
    private val crashLogger: CrashLogger,
    private val custodialWalletManager: CustodialWalletManager,
    rxBus: RxBus
) : BitcoinLikeTokens(rxBus) {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.BCH

    override fun defaultAccount(): Single<AccountReference> =
        with(bchDataManager) {
            val a = getAccountMetadataList()[getDefaultAccountPosition()]
            Single.just(a.toAccountReference())
        }

    override fun custodialBalanceMaybe(): Maybe<CryptoValue> =
        custodialWalletManager.getBalanceForAsset(CryptoCurrency.BCH)

    override fun noncustodialBalance(): Single<CryptoValue> =
        walletInitialiser()
            .andThen(Completable.defer { updater() })
            .toCryptoSingle(CryptoCurrency.BCH) { bchDataManager.getWalletBalance() }

    override fun balance(account: AccountReference): Single<CryptoValue> {
        val ref = accountReference(account)

        return walletInitialiser()
            .andThen(Completable.defer { updater() })
            .toCryptoSingle(CryptoCurrency.BCH) { bchDataManager.getAddressBalance(ref.xpub) }
    }

    override fun doUpdateBalances(): Completable =
        bchDataManager.updateAllBalances()
            .doOnComplete { Timber.d("Got btc balance") }

    override fun exchangeRate(): Single<FiatValue> =
        exchangeRates.fetchLastPrice(CryptoCurrency.BCH, currencyPrefs.selectedFiatCurrency)

    override fun historicRate(epochWhen: Long): Single<FiatValue> =
        exchangeRates.getHistoricPrice(CryptoCurrency.BCH, currencyPrefs.selectedFiatCurrency, epochWhen)

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        historicRates.getHistoricPriceSeries(CryptoCurrency.BCH, currencyPrefs.selectedFiatCurrency, period)

    private var isWalletUninitialised = true

    private fun walletInitialiser() =
        if (isWalletUninitialised) {
            bchDataManager.initBchWallet(stringUtils.getString(R.string.bch_default_account_label))
                .doOnError { throwable ->
                    crashLogger.logException(throwable, "Failed to load bch wallet")
                }.doOnComplete {
                    isWalletUninitialised = false
                }
        } else {
            Completable.complete()
        }

    override fun onLogoutSignal(event: AuthEvent) {
        isWalletUninitialised = true
        bchDataManager.clearBchAccountDetails()
        super.onLogoutSignal(event)
    }
}
