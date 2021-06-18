package piuk.blockchain.android.ui.base.mvi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.ui.base.BlockchainActivity
import timber.log.Timber

abstract class MviActivity<M : MviModel<S, I>, I : MviIntent<S>, S : MviState, E : ViewBinding> : BlockchainActivity() {

    protected abstract val model: M

    var subscription: Disposable? = null

    val binding: E by lazy {
        initBinding()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()
        subscription?.dispose()
        subscription = model.state.subscribeBy(
            onNext = { render(it) },
            onError = {
                if (BuildConfig.DEBUG) {
                    throw it
                }
                Timber.e(it)
            },
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

    abstract fun initBinding(): E

    protected abstract fun render(newState: S)

    companion object {
        inline fun <reified T : AppCompatActivity> start(ctx: Context) {
            ctx.startActivity(Intent(ctx, T::class.java))
        }
    }
}