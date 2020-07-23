package piuk.blockchain.android.ui.transfer.send

import android.view.View
import androidx.annotation.StringRes
import kotlinx.android.synthetic.main.dialog_send_prototype.view.cta_button
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import timber.log.Timber

abstract class FlowInputSheet(
    override val host: SlidingModalBottomDialog.Host
) : MviBottomSheet<SendModel, SendIntent, SendState>() {

    override val model: SendModel by sendInject()

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

class TransactionInProgressSheet(
    host: SlidingModalBottomDialog.Host
) : FlowInputSheet(host) {
    override val layoutResource: Int = R.layout.dialog_send_in_progress

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! TransactionInProgressSheet")
    }

    override fun initControls(view: View) {}
}

class TransactionCompleteSheet(
    host: SlidingModalBottomDialog.Host
) : FlowInputSheet(host) {
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
}