package piuk.blockchain.android.ui.base

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.viewbinding.ViewBinding
import com.blockchain.notifications.analytics.Analytics
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.android.ext.android.inject

abstract class SlidingModalBottomDialog<T : ViewBinding> : BottomSheetDialogFragment() {

    interface Host {
        fun onSheetClosed()
    }

    protected open val host: Host by lazy {
        parentFragment as? Host
            ?: activity as? Host
            ?: throw IllegalStateException("Host is not a SlidingModalBottomDialog.Host")
    }

    private var dismissed = false

    abstract fun initBinding(inflater: LayoutInflater, container: ViewGroup?): T

    private var _binding: T? = null

    val binding: T
        get() = _binding!!

    protected val analytics: Analytics by inject()

    final override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dlg = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        _binding = initBinding(layoutInflater, null)

        dlg.setContentView(binding.root)

        val bottomSheetBehavior = BottomSheetBehavior.from(binding.root.parent as View)

        dlg.setCanceledOnTouchOutside(false)

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(view: View, i: Int) {
                when (i) {
                    BottomSheetBehavior.STATE_EXPANDED -> onSheetExpanded()
                    BottomSheetBehavior.STATE_COLLAPSED -> onSheetCollapsed()
                    BottomSheetBehavior.STATE_HIDDEN -> onSheetHidden()
                    else -> { /* shouldn't get here! */
                    }
                }
            }

            override fun onSlide(view: View, v: Float) {}
        })

        initControls(binding)

        dlg.setOnShowListener {
            bottomSheetBehavior.skipCollapsed = true
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        return dlg
    }

    @CallSuper
    protected open fun onSheetHidden() {
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @CallSuper
    protected open fun onSheetExpanded() {
    }

    @CallSuper
    protected open fun onSheetCollapsed() {
    }

    abstract fun initControls(binding: T)

    // We use this dismissed flag to make sure that only one of onCancel or dismiss methods are called,
    // when the bottomsheet is dismissed in different ways.
    // When the bottomsheet is dismissed with the back button onCancel is called but not dismiss. On the hand
    // if bottomsheet is dismissed by code (.dismiss()) or with slide gesture then both methods are called.

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        if (dismissed) {
            return
        }
        dismissed = true
        host.onSheetClosed()
        resetSheetParent()
    }

    override fun dismiss() {
        super.dismiss()
        if (dismissed) {
            return
        }
        dismissed = true
        resetSheetParent()
        host.onSheetClosed()
    }

    private fun resetSheetParent() {
        val bottomSheetBehavior = BottomSheetBehavior.from(binding.root.parent as View)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }
}
