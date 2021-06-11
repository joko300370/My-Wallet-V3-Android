package com.blockchain.rx

import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class TimedCacheRequest<T>(
    private val cacheLifetimeSeconds: Long,
    private val refreshFn: () -> Single<T>
) {
    private val expired = AtomicBoolean(true)
    private var current = refreshFn.invoke()

    fun getCachedSingle(): Single<T> =
        Single.defer {
            if (expired.compareAndSet(true, false)) {
                current = refreshFn.invoke().cache().doOnError {
                    expired.set(true)
                }

                Single.timer(cacheLifetimeSeconds, TimeUnit.SECONDS)
                    .subscribeBy(onSuccess = { expired.set(true) })
            }
            current
        }
}

class ParameteredSingleTimedCacheRequest<INPUT, OUTPUT>(
    private val cacheLifetimeSeconds: Long,
    private val refreshFn: (INPUT) -> Single<OUTPUT>
) {
    private val expired = hashMapOf<INPUT, Boolean>()
    private lateinit var current: Single<OUTPUT>

    fun getCachedSingle(input: INPUT): Single<OUTPUT> =
        Single.defer {
            if (expired[input] != false) {
                current = refreshFn.invoke(input).cache().doOnSuccess {
                    expired[input] = false
                }.doOnError {
                    expired[input] = true
                }

                Single.timer(cacheLifetimeSeconds, TimeUnit.SECONDS)
                    .subscribeBy(onSuccess = { expired[input] = true })
            }
            current
        }

    fun invalidate(input: INPUT) {
        expired[input] = true
    }
}

class ParameteredMappedSinglesTimedRequests<INPUT, OUTPUT>(
    private val cacheLifetimeSeconds: Long,
    private val refreshFn: (INPUT) -> Single<OUTPUT>
) {
    private val expired = hashMapOf<INPUT, Boolean>()
    private val values = hashMapOf<INPUT, Single<OUTPUT>>()

    fun getCachedSingle(input: INPUT): Single<OUTPUT> =
        Single.defer {
            if (expired[input] != false) {
                values[input] = refreshFn.invoke(input).cache().doOnSuccess {
                    expired[input] = false
                }.doOnError {
                    expired[input] = true
                }

                Single.timer(cacheLifetimeSeconds, TimeUnit.SECONDS)
                    .subscribeBy(onSuccess = { expired[input] = true })
            }
            values[input]
        }

    fun invalidate(input: INPUT) {
        expired[input] = true
    }
}