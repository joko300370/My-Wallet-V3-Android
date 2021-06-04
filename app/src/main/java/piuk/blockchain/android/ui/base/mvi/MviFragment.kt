package piuk.blockchain.android.ui.base.mvi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.blockchain.notifications.analytics.Analytics
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.ui.base.BlockchainActivity
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import timber.log.Timber

abstract class MviFragment<M : MviModel<S, I>, I : MviIntent<S>, S : MviState, E : ViewBinding> : Fragment() {

    protected abstract val model: M

    var subscription: Disposable? = null

    private var _binding: E? = null

    val binding get() = _binding!!

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = initBinding(inflater, container)
        return binding.root
    }

    override fun onDestroy() {
        model.destroy()
        super.onDestroy()
        _binding = null
    }

    abstract fun initBinding(inflater: LayoutInflater, container: ViewGroup?): E

    protected abstract fun render(newState: S)

    protected val activity: BlockchainActivity
        get() = requireActivity() as? BlockchainActivity
            ?: throw IllegalStateException("Root activity is not a BlockchainActivity")

    protected val analytics: Analytics
        get() = activity.analytics

    @UiThread
    protected fun showAlert(dlg: AlertDialog) = activity.showAlert(dlg)

    @UiThread
    protected fun clearAlert() = activity.clearAlert()

    @UiThread
    fun showProgressDialog(@StringRes messageId: Int, onCancel: (() -> Unit)? = null) =
        activity.showProgressDialog(messageId, onCancel)

    @UiThread
    fun dismissProgressDialog() = activity.dismissProgressDialog()

    @UiThread
    fun updateProgressDialog(msg: String) = activity.updateProgressDialog(msg)

    @UiThread
    fun showBottomSheet(bottomSheet: BottomSheetDialogFragment?) =
        bottomSheet?.show(childFragmentManager, BOTTOM_SHEET)

    @UiThread
    fun clearBottomSheet() {
        val dlg = childFragmentManager.findFragmentByTag(BOTTOM_SHEET)

        dlg?.let {
            (it as? SlidingModalBottomDialog<ViewBinding>)?.dismiss()
                ?: throw IllegalStateException("Fragment is not a $BOTTOM_SHEET")
        }
    }

    companion object {
        const val BOTTOM_SHEET = "BOTTOM_SHEET"
    }
}
