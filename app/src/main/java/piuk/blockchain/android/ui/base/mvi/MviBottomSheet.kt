package piuk.blockchain.android.ui.base.mvi

import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import timber.log.Timber

abstract class MviBottomSheet<M : MviModel<S, I>, I : MviIntent<S>, S : MviState> : SlidingModalBottomDialog() {

    protected abstract val model: M

    var subscription: Disposable? = null

    override fun onResume() {
        super.onResume()
        subscription?.dispose()
        subscription = model.state.subscribeBy(
                onNext = { render(it) },
                onError = { Timber.e(it) },
                onComplete = { Timber.d("***> State on complete!!") }
        )
    }

    override fun onPause() {
        subscription?.dispose()
        subscription = null
        super.onPause()
    }

    override fun onDestroy() {
        model.destroy()
        super.onDestroy()
    }

    protected abstract fun render(newState: S)
}
