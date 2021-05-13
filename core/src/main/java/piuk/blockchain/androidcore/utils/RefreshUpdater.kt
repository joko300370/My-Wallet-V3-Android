package piuk.blockchain.androidcore.utils

import io.reactivex.Completable
import io.reactivex.Single
import java.util.concurrent.atomic.AtomicLong

class RefreshUpdater<T>(
    private val fnRefresh: () -> Completable,
    private val refreshInterval: Long = THIRTY_SECONDS
) {
    private val lastRefreshTime = AtomicLong(0)

    fun get(
        fnFetch: () -> T,
        force: Boolean = false
    ): Single<T> {
        val now = System.currentTimeMillis()

        return if (force || (now - refreshInterval > lastRefreshTime.get())) {
            lastRefreshTime.set(System.currentTimeMillis())
            fnRefresh.invoke()
                .toSingle { fnFetch.invoke() }
        } else {
            Single.fromCallable { fnFetch() }
        }
    }

    fun reset() {
        lastRefreshTime.set(0)
    }

    companion object {
        private const val THIRTY_SECONDS: Long = 30 * 1000
    }
}
