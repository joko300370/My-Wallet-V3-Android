package piuk.blockchain.android.ui.base

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import com.blockchain.notifications.analytics.Analytics
import java.lang.IllegalStateException

abstract class MvpFragment<V : MvpView, P : MvpPresenter<V>> : Fragment() {

    protected abstract val presenter: P
    protected abstract val view: V

    @CallSuper
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        presenter.attachView(view)
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        presenter.onViewResumed()
    }

    @CallSuper
    override fun onPause() {
        presenter.onViewPaused()
        super.onPause()
    }

    @CallSuper
    override fun onDestroy() {
        presenter.detachView(view)
        super.onDestroy()
    }

    protected fun onViewReady() {
        presenter.onViewReady()
    }

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
    fun showBottomSheet(bottomSheet: BottomSheetDialogFragment) =
        activity.showBottomSheet(bottomSheet)

    protected open fun initViewOnResume(): Boolean = true
    protected open fun clearViewOnPause(): Boolean = true
}
