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
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import java.lang.IllegalArgumentException

class ETHTokens(
    private val ethDataManager: EthDataManager,
    private val exchangeRates: ExchangeRateDataManager,
    private val historicRates: ChartsDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val stringUtils: StringUtils,
    private val crashLogger: CrashLogger,
    private val custodialWalletManager: CustodialWalletManager,
    rxBus: RxBus
) : AssetTokensBase(rxBus) {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.ETHER

    override fun defaultAccount(): Single<AccountReference> =
        Single.just(getDefaultEthAccountRef())

    private fun getDefaultEthAccountRef(): AccountReference =
        ethDataManager.getEthWallet()?.account?.toAccountReference()
            ?: throw Exception("No ether wallet found")

    override fun custodialBalanceMaybe(): Maybe<CryptoValue> =
        custodialWalletManager.getBalanceForAsset(CryptoCurrency.ETHER)

    override fun noncustodialBalance(): Single<CryptoValue> =
        etheriumWalletInitialiser()
            .andThen(ethDataManager.fetchEthAddress())
            .singleOrError()
            .map { CryptoValue(CryptoCurrency.ETHER, it.getTotalBalance()) }

    override fun balance(account: AccountReference): Single<CryptoValue> {
        val ref = account as? AccountReference.Ethereum
            ?: throw IllegalArgumentException("Not an XLM Account Ref")

        return etheriumWalletInitialiser()
            .andThen(ethDataManager.getBalance(ref.address))
            .map { CryptoValue.etherFromWei(it) }
    }

    override fun exchangeRate(): Single<FiatValue> =
        exchangeRates.fetchLastPrice(CryptoCurrency.ETHER, currencyPrefs.selectedFiatCurrency)

    override fun historicRate(epochWhen: Long): Single<FiatValue> =
        exchangeRates.getHistoricPrice(CryptoCurrency.ETHER, currencyPrefs.selectedFiatCurrency, epochWhen)

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        historicRates.getHistoricPriceSeries(CryptoCurrency.ETHER, currencyPrefs.selectedFiatCurrency, period)

    private var isWalletUninitialised = true

    private fun etheriumWalletInitialiser() =
        if (isWalletUninitialised) {
            ethDataManager.initEthereumWallet(
                stringUtils.getString(R.string.eth_default_account_label),
                stringUtils.getString(R.string.pax_default_account_label)
            ).doOnError { throwable ->
                crashLogger.logException(throwable, "Failed to load ETH wallet")
            }.doOnComplete {
                isWalletUninitialised = false
            }
        } else {
            Completable.complete()
        }

    override fun onLogoutSignal(event: AuthEvent) {
        isWalletUninitialised = true
        ethDataManager.clearEthAccountDetails()
    }
}
