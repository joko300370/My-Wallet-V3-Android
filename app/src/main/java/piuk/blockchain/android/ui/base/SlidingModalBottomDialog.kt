package piuk.blockchain.android.ui.base

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.CallSuper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.View
import com.blockchain.notifications.analytics.Analytics
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.koin.android.ext.android.inject

abstract class SlidingModalBottomDialog : BottomSheetDialogFragment() {

    interface Host {
        fun onSheetClosed()
    }

    protected open val host: Host by lazy {
        parentFragment as? Host
            ?: activity as? Host
            ?: throw IllegalStateException("Host is not a SlidingModalBottomDialog.Host")
    }

    protected lateinit var dialogView: View

    protected val analytics: Analytics by inject()

    final override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dlg = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        val view = View.inflate(context, layoutResource, null)
        dlg.setContentView(view)

        val bottomSheetBehavior = BottomSheetBehavior.from(view.parent as View)

        bottomSheetBehavior.setBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(view: View, i: Int) {
                when (i) {
                    BottomSheetBehavior.STATE_EXPANDED -> onSheetExpanded()
                    BottomSheetBehavior.STATE_COLLAPSED -> onSheetCollapsed()
                    BottomSheetBehavior.STATE_HIDDEN -> onSheetHidden()
                    else -> { /* shouldn't get here! */ }
                }
            }

            override fun onSlide(view: View, v: Float) {}
        })

        dialogView = view
        initControls(view)

        dlg.setOnShowListener {
            bottomSheetBehavior.skipCollapsed = true
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        return dlg
    }

    @CallSuper
    protected open fun onSheetHidden() {
        host.onSheetClosed()
        dismiss()
    }

    @CallSuper
    protected open fun onSheetExpanded() { }

    @CallSuper
    protected open fun onSheetCollapsed() { }

    protected abstract val layoutResource: Int
    protected abstract fun initControls(view: View)

    override fun onCancel(dialog: DialogInterface) {
        host.onSheetClosed()
        super.onCancel(dialog)
    }

    override fun dismiss() {
        host.onSheetClosed()
        super.dismiss()
    }
}
