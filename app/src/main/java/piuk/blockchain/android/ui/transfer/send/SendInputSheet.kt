package piuk.blockchain.android.ui.transfer.send

import androidx.annotation.StringRes
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

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