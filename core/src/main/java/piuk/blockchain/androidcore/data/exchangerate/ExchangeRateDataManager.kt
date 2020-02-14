package piuk.blockchain.androidcore.data.exchangerate

import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.exchangerate.datastore.ExchangeRateDataStore
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.rxjava.RxPinning
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * This data manager is responsible for storing and updating the latest exchange rates information
 * for all crypto currencies.
 * Historic prices for all crypto currencies can be queried from here.
 */
class ExchangeRateDataManager(
    private val exchangeRateDataStore: ExchangeRateDataStore,
    rxBus: RxBus
) {

    private val rxPinning = RxPinning(rxBus)

    fun updateTickers(): Completable =
        rxPinning.call { exchangeRateDataStore.updateExchangeRates() }
            .subscribeOn(Schedulers.io())

    fun getLastPrice(cryptoCurrency: CryptoCurrency, currencyName: String) =
        exchangeRateDataStore.getLastPrice(cryptoCurrency, currencyName)

    fun getLastPriceOfFiat(targetCurrency: String, sourceCurrency: String) =
        exchangeRateDataStore.getFiatLastPrice(targetCurrency = targetCurrency, sourceCurrency = sourceCurrency)

    fun getHistoricPrice(value: CryptoValue, fiat: String, timeInSeconds: Long): Single<FiatValue> =
        exchangeRateDataStore.getHistoricPrice(value.currency, fiat, timeInSeconds)
            .map { FiatValue.fromMajor(fiat, it * value.toBigDecimal()) }
            .subscribeOn(Schedulers.io())

    fun getHistoricPrice(currency: CryptoCurrency, fiat: String, timeInSeconds: Long): Single<FiatValue> =
        exchangeRateDataStore.getHistoricPrice(currency, fiat, timeInSeconds)
            .map { FiatValue.fromMajor(fiat, it) }
            .subscribeOn(Schedulers.io())

    fun getCurrencyLabels() = exchangeRateDataStore.getCurrencyLabels()

    @Deprecated("Use CryptoValue.toFiat")
    fun getFiatFromBtc(btc: BigDecimal, fiatUnit: String): BigDecimal {
        return getLastPrice(CryptoCurrency.BTC, fiatUnit).toBigDecimal() * btc
    }

    @Deprecated("Use CryptoValue.toFiat")
    fun getFiatFromBch(bch: BigDecimal, fiatUnit: String): BigDecimal {
        return getLastPrice(CryptoCurrency.BCH, fiatUnit).toBigDecimal() * bch
    }

    @Deprecated("Use CryptoValue.toFiat")
    fun getFiatFromEth(eth: BigDecimal, fiatUnit: String): BigDecimal {
        return getLastPrice(CryptoCurrency.ETHER, fiatUnit).toBigDecimal() * eth
    }
}

fun CryptoValue.toFiat(exchangeRateDataManager: ExchangeRateDataManager, fiatUnit: String) =
    FiatValue.fromMajor(
        fiatUnit,
        exchangeRateDataManager.getLastPrice(currency, fiatUnit).toBigDecimal() * toBigDecimal()
    )

fun FiatValue.toFiatWithCurrency(exchangeRateDataManager: ExchangeRateDataManager, targetCurrency: String) =
    FiatValue.fromMajor(targetCurrency,
        exchangeRateDataManager.getLastPriceOfFiat(sourceCurrency =
        this.currencyCode, targetCurrency = targetCurrency
        ).toBigDecimal() * toBigDecimal()
    )

fun FiatValue.toCrypto(exchangeRateDataManager: ExchangeRateDataManager, cryptoCurrency: CryptoCurrency) =
    toCryptoOrNull(exchangeRateDataManager, cryptoCurrency) ?: CryptoValue.zero(cryptoCurrency)

fun FiatValue.toCryptoOrNull(exchangeRateDataManager: ExchangeRateDataManager, cryptoCurrency: CryptoCurrency) =
    if (isZero) {
        CryptoValue.zero(cryptoCurrency)
    } else {
        val rate = exchangeRateDataManager.getLastPrice(cryptoCurrency, this.currencyCode).toBigDecimal()
        if (rate.signum() == 0) {
            null
        } else {
            CryptoValue.fromMajor(
                cryptoCurrency,
                toBigDecimal().divide(rate, cryptoCurrency.dp, RoundingMode.HALF_UP)
            )
        }
    }

/**
 * Exchange rates for a single fiat currency.
 * Saves passing around a fiat currency string, or looking up the users preferred currency.
 */

class FiatExchangeRates constructor(
    internal val exchangeRates: ExchangeRateDataManager,
    internal val fiatUnit: String
) {
    constructor(exchangeRates: ExchangeRateDataManager, currencyPrefs: CurrencyPrefs) :
            this(exchangeRates, currencyPrefs.selectedFiatCurrency)

    fun getFiat(cryptoValue: CryptoValue): FiatValue = cryptoValue.toFiat(exchangeRates, fiatUnit)

    fun getCrypto(fiatValue: FiatValue, cryptoCurrency: CryptoCurrency): CryptoValue =
        fiatValue.toCrypto(exchangeRates, cryptoCurrency)
}

fun CryptoValue.toFiat(liveFiatExchangeRates: FiatExchangeRates) =
    liveFiatExchangeRates.getFiat(this)

fun FiatValue.toCrypto(liveFiatExchangeRates: FiatExchangeRates, cryptoCurrency: CryptoCurrency) =
    liveFiatExchangeRates.getCrypto(this, cryptoCurrency)
