package piuk.blockchain.androidcore.utils

import io.reactivex.Completable
import io.reactivex.Single

class RefreshUpdater<T>(
    private val fnRefresh: () -> Completable,
    private val refreshInterval: Long = THIRTY_SECONDS
) {

    private var lastRefreshTime: Long = 0

    fun get(
        fnFetch: () -> T,
        force: Boolean = false
    ): Single<T> {
        val now = System.currentTimeMillis()

        return if (force || (now > lastRefreshTime + refreshInterval)) {
            fnRefresh()
                .doOnComplete { lastRefreshTime = System.currentTimeMillis() }
                .toSingle { fnFetch.invoke() }
        } else {
            Single.fromCallable { fnFetch() }
        }
    }

    fun reset() {
        lastRefreshTime = 0
    }

    companion object {
        private const val THIRTY_SECONDS: Long = 30 * 1000
    }
}
