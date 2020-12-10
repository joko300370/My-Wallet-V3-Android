package piuk.blockchain.android.ui.base.mvi

import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable

import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.ReplaySubject
import timber.log.Timber

interface MviState

interface MviIntent<S : MviState> {
    fun reduce(oldState: S): S
    fun isValidFor(oldState: S): Boolean = true
}

abstract class MviModel<S : MviState, I : MviIntent<S>>(
    initialState: S,
    observeScheduler: Scheduler
) {

    private val _state: BehaviorRelay<S> = BehaviorRelay.createDefault(initialState)
    val state: Observable<S> = _state.distinctUntilChanged().doOnNext {
        onStateUpdate(it)
    }.observeOn(observeScheduler)

    protected val disposables = CompositeDisposable()
    private val intents = ReplaySubject.create<I>()

    init {
        disposables +=
            intents.distinctUntilChanged(::distinctIntentFilter)
                .observeOn(Schedulers.computation())
                .scan(initialState) { previousState, intent ->
                    Timber.d("***> Model: ProcessIntent: ${intent.javaClass.simpleName}")
                    if (intent.isValidFor(previousState)) {
                        performAction(previousState, intent)?.let { disposables += it }
                        intent.reduce(previousState)
                    } else {
                        Timber.d("***> Model: Dropping invalid Intent: ${intent.javaClass.simpleName}")
                        previousState
                    }
                }
                .subscribeOn(Schedulers.computation())
                .subscribeBy(
                    onNext = { newState ->
                        _state.accept(newState)
                    },
                    onError = ::onScanLoopError
                )
    }

    fun process(intent: I) = intents.onNext(intent)
    fun destroy() {
        disposables.clear()
    }

    protected open fun distinctIntentFilter(previousIntent: I, nextIntent: I): Boolean {
        return previousIntent == nextIntent
    }

    protected open fun onScanLoopError(t: Throwable) {}
    protected open fun onStateUpdate(s: S) {}

    protected abstract fun performAction(previousState: S, intent: I): Disposable?
}
