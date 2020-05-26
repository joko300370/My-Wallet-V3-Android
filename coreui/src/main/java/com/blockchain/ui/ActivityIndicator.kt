package com.blockchain.ui

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.locks.ReentrantLock

class ActivityIndicator {

    private val lock = ReentrantLock()
    private val variable = BehaviorSubject.createDefault(0)

    val loading: Observable<Boolean> = variable.map { it > 0 }.distinctUntilChanged()
        .share()
        .replay(1)
        .refCount()

    fun <T> Observable<T>.trackLoading(): Observable<T> {
        return this.doOnSubscribe {
            increment()
        }.doFinally {
            decrement()
        }
    }

    fun <T> Single<T>.trackLoading(): Single<T> {
        return this.doOnSubscribe {
            increment()
        }.doFinally {
            decrement()
        }
    }

    fun Completable.trackLoading(): Completable {
        return this.doOnSubscribe {
            increment()
        }.doFinally {
            decrement()
        }
    }

    private fun increment() {
        lock.lock()
        variable.onNext(variable.value?.inc() ?: 1)
        lock.unlock()
    }

    private fun decrement() {
        lock.lock()
        variable.onNext(variable.value?.dec() ?: 0)
        lock.unlock()
    }
}

fun <T> Observable<T>.trackLoading(activityIndicator: ActivityIndicator?): Observable<T> =
    activityIndicator?.let {
        with(it) {
            trackLoading()
        }
    } ?: this

fun <T> Single<T>.trackLoading(activityIndicator: ActivityIndicator?): Single<T> =
    activityIndicator?.let {
        with(it) {
            trackLoading()
        }
    } ?: this

fun Completable.trackLoading(activityIndicator: ActivityIndicator?): Completable =
    activityIndicator?.let {
        with(it) {
            trackLoading()
        }
    } ?: this