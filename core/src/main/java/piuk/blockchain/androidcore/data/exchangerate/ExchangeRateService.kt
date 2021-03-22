package piuk.blockchain.androidcore.data.exchangerate

import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.prices.PriceApi
import info.blockchain.wallet.prices.TimeInterval
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.rxjava.RxPinning
import java.util.Calendar

enum class TimeSpan {
    DAY,
    WEEK,
    MONTH,
    YEAR,
    ALL_TIME
}

/**
 * All time start times in epoch-seconds
 */

typealias PriceSeries = List<PriceDatum>

class ExchangeRateService(private val priceApi: PriceApi, rxBus: RxBus) {
    private val rxPinning = RxPinning(rxBus)

    fun getExchangeRateMap(cryptoCurrency: CryptoCurrency): Single<Map<String, PriceDatum>> =
        priceApi.getPriceIndexes(cryptoCurrency.networkTicker)

    fun getHistoricPrice(
        cryptoCurrency: CryptoCurrency,
        currency: String,
        timeInSeconds: Long
    ): Single<Double> =
        priceApi.getHistoricPrice(cryptoCurrency.networkTicker, currency, timeInSeconds)

    fun getHistoricPriceSeries(
        cryptoCurrency: CryptoCurrency,
        fiatCurrency: String,
        timeSpan: TimeSpan,
        timeInterval: TimeInterval = suggestedTimeIntervalForSpan(timeSpan)
    ): Single<PriceSeries> {

        var proposedStartTime = getStartTimeForTimeSpan(timeSpan, cryptoCurrency)
        // It's possible that the selected start time is before the currency existed, so check here
        // and show ALL_TIME instead if that's the case.
        if (proposedStartTime < cryptoCurrency.startDateForPrice) {
            proposedStartTime = getStartTimeForTimeSpan(TimeSpan.ALL_TIME, cryptoCurrency)
        }

        return rxPinning.callSingle<PriceSeries> {
            priceApi.getHistoricPriceSeries(
                cryptoCurrency.networkTicker,
                fiatCurrency,
                proposedStartTime,
                timeInterval.intervalSeconds
            ).subscribeOn(Schedulers.io())
        }
    }

    /**
     * Provides the first timestamp for which we have prices, returned in epoch-seconds
     *
     * @param cryptoCurrency The [CryptoCurrency] that you want a start date for
     * @return A [Long] in epoch-seconds since the start of our data
     */
    private fun getStartTimeForTimeSpan(
        timeSpan: TimeSpan,
        cryptoCurrency: CryptoCurrency
    ): Long {
        val start = when (timeSpan) {
            TimeSpan.ALL_TIME -> return cryptoCurrency.startDateForPrice
            TimeSpan.YEAR -> 365
            TimeSpan.MONTH -> 30
            TimeSpan.WEEK -> 7
            TimeSpan.DAY -> 1
        }

        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -start) }
        return cal.timeInMillis / 1000
    }

    private fun suggestedTimeIntervalForSpan(timeSpan: TimeSpan): TimeInterval =
        when (timeSpan) {
            TimeSpan.ALL_TIME -> TimeInterval.FIVE_DAYS
            TimeSpan.YEAR -> TimeInterval.ONE_DAY
            TimeSpan.MONTH -> TimeInterval.TWO_HOURS
            TimeSpan.WEEK -> TimeInterval.ONE_HOUR
            TimeSpan.DAY -> TimeInterval.FIFTEEN_MINUTES
        }
}