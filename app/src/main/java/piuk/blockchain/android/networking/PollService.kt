package piuk.blockchain.android.networking

import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import java.util.concurrent.TimeUnit

sealed class PollResult<T>(val value: T) {
    class FinalResult<T>(value: T) : PollResult<T>(value)
    class TimeOut<T>(value: T) : PollResult<T>(value)
}

class PollService<T : Any>(
    private val fetcher: Single<T>,
    private val matcher: (T) -> Boolean
) {

    fun start(timerInSec: Long = 5, retries: Int = 20) =
        fetcher.repeatWhen { it.delay(timerInSec, TimeUnit.SECONDS).zipWith(Flowable.range(0, retries)) }
            .takeUntil {
                matcher(it)
            }
            .lastOrError()
            .map {
                if (matcher(it))
                    PollResult.FinalResult(it)
                else
                    PollResult.TimeOut(it)
            }
}