package piuk.blockchain.android.ui.transfer.send

import android.view.View
import androidx.annotation.StringRes
import kotlinx.android.synthetic.main.dialog_send_prototype.view.cta_button
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import timber.log.Timber

abstract class SendInputSheet : MviBottomSheet<SendModel, SendIntent, SendState>() {
    override val model: SendModel by sendInject()

    override fun onSheetHidden() {
        super.onSheetHidden()
        dialog?.let {
            onCancel(it)
        }
    }

    protected fun showErrorToast(@StringRes msgId: Int) {
        ToastCustom.makeText(
            activity,
            getString(msgId),
            ToastCustom.LENGTH_LONG,
            ToastCustom.TYPE_ERROR
        )
    }

    @Deprecated(message = "For dev only, use resourecID version in production code")
    protected fun showErrorToast(msg: String) {
        ToastCustom.makeText(
            activity,
            msg,
            ToastCustom.LENGTH_LONG,
            ToastCustom.TYPE_ERROR
        )
    }
}

class TransactionInProgressSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_in_progress

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! TransactionInProgressSheet")
    }

    override fun initControls(view: View) {}

    companion object {
        fun newInstance(): TransactionInProgressSheet =
            TransactionInProgressSheet()
    }
}

class TransactionCompleteSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_complete

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! TransactionCompleteSheet")
    }

    override fun initControls(view: View) {
        view.cta_button.setOnClickListener { onCtaClick() }
    }

    private fun onCtaClick() {
        dismiss()
    }

    companion object {
        fun newInstance(): TransactionCompleteSheet =
            TransactionCompleteSheet()
    }
}