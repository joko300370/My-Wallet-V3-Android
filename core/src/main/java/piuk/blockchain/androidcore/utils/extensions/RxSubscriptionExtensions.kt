package piuk.blockchain.androidcore.utils.extensions

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.MaybeSource
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.annotations.CheckReturnValue
import io.reactivex.annotations.SchedulerSupport
import io.reactivex.disposables.Disposable
import io.reactivex.exceptions.OnErrorNotImplementedException
import io.reactivex.internal.functions.Functions

/**
 * Subscribes to a [Maybe] and silently consumes any emitted values. Any exceptions thrown won't
 * cascade into a [OnErrorNotImplementedException], but will be signalled to the RxJava plugin
 * error handler. Note that this means that [RxJavaPlugins.setErrorHandler()] MUST be set.
 *
 * @return A [Disposable] object.
 */
fun <T> Maybe<T>.emptySubscribe(): Disposable =
    subscribe(Functions.emptyConsumer(), Functions.ERROR_CONSUMER)

/**
 * Subscribes to a [Maybe] and silently consumes any emitted values. Any exceptions thrown won't
 * cascade into a [OnErrorNotImplementedException], but will be signalled to the RxJava plugin
 * error handler. Note that this means that [RxJavaPlugins.setErrorHandler()] MUST be set.
 *
 * @return A [Disposable] object.
 */
fun <T> Single<T>.emptySubscribe(): Disposable =
    subscribe(Functions.emptyConsumer(), Functions.ERROR_CONSUMER)

/**
 * Subscribes to a [Flowable] and silently consumes any emitted values. Any exceptions thrown won't
 * cascade into a [OnErrorNotImplementedException], but will be signalled to the RxJava plugin
 * error handler. Note that this means that [RxJavaPlugins.setErrorHandler()] MUST be set.
 *
 * @return A [Disposable] object.
 */
fun <T> Flowable<T>.emptySubscribe(): Disposable =
    subscribe(Functions.emptyConsumer(), Functions.ERROR_CONSUMER)

/**
 * Subscribes to a [Observable] and silently consumes any emitted values. Any exceptions thrown won't
 * cascade into a [OnErrorNotImplementedException], but will be signalled to the RxJava plugin
 * error handler. Note that this means that [RxJavaPlugins.setErrorHandler()] MUST be set.
 *
 * @return A [Disposable] object.
 */
fun <T> Observable<T>.emptySubscribe(): Disposable =
    subscribe(Functions.emptyConsumer(), Functions.ERROR_CONSUMER)

/**
 * Subscribes to a [Completable] and silently completes, if applicable. Any exceptions thrown won't
 * cascade into a [OnErrorNotImplementedException], but will be signalled to the RxJava plugin
 * error handler. Note that this means that [RxJavaPlugins.setErrorHandler()] MUST be set.
 *
 * @return A [Disposable] object.
 */
fun Completable.emptySubscribe(): Disposable =
    subscribe(Functions.EMPTY_ACTION, Functions.ERROR_CONSUMER)

/**
 * Get a "this is ambiguous" error using Maybe.switchIfEmpty(): Single on a maybe in Kotlin. So fix it here!
 */

@CheckReturnValue
@SchedulerSupport(SchedulerSupport.NONE)
fun <T> Maybe<T>.switchToSingleIfEmpty(other: SingleSource<out T>?): Single<T> =
    switchIfEmpty(other)

@CheckReturnValue
@SchedulerSupport(SchedulerSupport.NONE)
fun <T> Maybe<T>.switchToSingleIfEmpty(source: () -> SingleSource<out T>?): Single<T> =
    switchIfEmpty(source())

@CheckReturnValue
@SchedulerSupport(SchedulerSupport.NONE)
fun <T, R> Maybe<T>.flatMapBy(
    onSuccess: (T) -> MaybeSource<out R>?,
    onError: (Throwable?) -> MaybeSource<out R>?,
    onComplete: () -> MaybeSource<out R>?
): Maybe<R> = this.flatMap(
    onSuccess,
    onError,
    onComplete
)