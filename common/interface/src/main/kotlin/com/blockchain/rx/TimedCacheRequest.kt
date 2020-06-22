package com.blockchain.rx

import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class TimedCacheRequest<T>(
    private val cacheLifetimeSeconds: Long,
    private val refreshFn: () -> Single<T>
) {
    val expired = AtomicBoolean(true)
    var current = refreshFn.invoke()

    fun getCachedSingle(): Single<T> =
        Single.defer {
            if (expired.compareAndSet(true, false)) {
                current = refreshFn.invoke().cache()

                Single.timer(cacheLifetimeSeconds, TimeUnit.SECONDS)
                    .subscribeBy(onSuccess = { expired.set(true) })
            }
        current
        }
}
