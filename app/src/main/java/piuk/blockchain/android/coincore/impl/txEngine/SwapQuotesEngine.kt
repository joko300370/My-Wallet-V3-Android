package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.swap.nabu.datamanagers.CurrencyPair
import com.blockchain.swap.nabu.datamanagers.Direction
import com.blockchain.swap.nabu.datamanagers.SwapQuote
import com.blockchain.swap.nabu.datamanagers.repositories.QuotesProvider
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import piuk.blockchain.android.coincore.impl.PricesInterpolator
import java.math.BigDecimal
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

class SwapQuotesEngine(
    private val quotesProvider: QuotesProvider,
    private val direction: Direction,
    private val pair: CurrencyPair.CryptoCurrencyPair
) {
    private lateinit var latestQuote: SwapQuote

    private val amount = BehaviorSubject.createDefault<BigDecimal>(BigDecimal.ZERO)

    val quote = quotesProvider.fetchQuote(direction = direction, pair = pair).flatMapObservable { quote ->
        quotesProvider.fetchQuote(direction = direction, pair = pair)
            .delay(quote.creationDate.diffInSeconds(quote.expirationDate), TimeUnit.SECONDS)
            .repeat().toObservable().startWith(quote)
    }.doOnNext {
        latestQuote = it
    }.share().replay(1).refCount()

    fun getRate(): Observable<BigDecimal> =
        Observables.combineLatest(quote.map {
            PricesInterpolator(
                list = it.prices
            )
        }, amount).map { (interpolator, amount) ->
            interpolator.getRate(amount)
        }

    fun getLatestQuote(): SwapQuote = latestQuote

    fun updateAmount(newAmount: BigDecimal) = amount.onNext(newAmount)
}

private fun Date.diffInSeconds(other: Date): Long = (this.time - other.time).absoluteValue / 100