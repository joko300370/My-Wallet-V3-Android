package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.nabu.datamanagers.TransferDirection
import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.nabu.datamanagers.TransferQuote
import com.blockchain.nabu.datamanagers.repositories.QuotesProvider
import info.blockchain.balance.Money
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import piuk.blockchain.android.coincore.impl.PricesInterpolator
import java.math.BigInteger
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

class TransferQuotesEngine(
    private val quotesProvider: QuotesProvider
) {
    private lateinit var latestQuote: PricedQuote

    private lateinit var direction: TransferDirection
    private lateinit var pair: CurrencyPair

    private val stop = PublishSubject.create<Unit>()

    private val amount = BehaviorSubject.createDefault<Money>(pair.toSourceMoney(BigInteger.ZERO))

    private val quote: Observable<TransferQuote> =
        quotesProvider.fetchQuote(direction = direction, pair = pair).flatMapObservable { quote ->
            Observable.interval(
                quote.creationDate.diffInSeconds(quote.expirationDate),
                quote.creationDate.diffInSeconds(quote.expirationDate),
                TimeUnit.SECONDS
            ).flatMapSingle {
                quotesProvider.fetchQuote(direction = direction, pair = pair)
            }.startWith(quote)
        }.takeUntil(stop)

    val pricedQuote: Observable<PricedQuote> = Observables.combineLatest(quote, amount).map { (quote, amount) ->
        PricedQuote(PricesInterpolator(
            list = quote.prices,
            pair = pair
        ).getRate(amount), quote)
    }.doOnNext {
        latestQuote = it
    }.share().replay(1).refCount()

    fun start(
        direction: TransferDirection,
        pair: CurrencyPair
    ) {
        // This is a bit of a hack, to make the clients of this code testable.
        // start() can be called multiple times by a given client, but we only care about the first.
        // Since the params should NOT change between invocations, we will enforce it as an invariant.
        // TODO: Fix this properly, once the test framework is all in place.
        if (this::direction.isInitialized) {
            require(this.direction == direction)
        } else {
            this.direction = direction
        }
        if (this::pair.isInitialized) {
            require(this.pair == pair)
        } else {
            this.pair = pair
        }
    }

    fun stop() {
        stop.onNext(Unit)
    }

    fun getLatestQuote(): PricedQuote = latestQuote

    fun updateAmount(newAmount: Money) = amount.onNext(newAmount)
}

data class PricedQuote(val price: Money, val transferQuote: TransferQuote)

private fun Date.diffInSeconds(other: Date): Long = (this.time - other.time).absoluteValue / 100