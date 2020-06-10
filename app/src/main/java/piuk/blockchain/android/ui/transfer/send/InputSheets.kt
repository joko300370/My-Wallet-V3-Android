package piuk.blockchain.android.ui.transfer.send

import android.view.View
import androidx.annotation.StringRes
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import kotlinx.android.synthetic.main.dialog_send_prototype.view.*
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

class EnterSecondPasswordSheet : SendInputSheet() {

    override val layoutResource: Int = R.layout.dialog_send_password

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! EnterSecondPasswordSheet")
    }

    override fun initControls(view: View) {
        view.cta_button.setOnClickListener { onCtaClick() }
    }

    private fun onCtaClick() {
        model.process(SendIntent.ValidatePassword("second password"))
    }

    companion object {
        fun newInstance(): EnterSecondPasswordSheet =
            EnterSecondPasswordSheet()
    }
}

class EnterAmountSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_enter_amount

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! EnterAmountSheet")
    }

    override fun initControls(view: View) {
        view.cta_button.setOnClickListener { onCtaClick() }
    }

    private fun onCtaClick() {
        model.process(SendIntent.PrepareTransaction(
            CryptoValue.fromMinor(CryptoCurrency.ETHER, 1000000.toBigDecimal()))
        )
    }

    companion object {
        fun newInstance(): EnterAmountSheet =
            EnterAmountSheet()
    }
}

class ConfirmTransactionSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_confirm

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! ConfirmTransactionSheet")
    }

    override fun initControls(view: View) {
        view.cta_button.setOnClickListener { onCtaClick() }
    }

    private fun onCtaClick() {
        model.process(SendIntent.ExecuteTransaction)
    }

    companion object {
        fun newInstance(): ConfirmTransactionSheet =
            ConfirmTransactionSheet()
    }
}

class TransactionInProgressSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_in_progress

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! TransactionInProgressSheet")
    }

    override fun initControls(view: View) { }

    companion object {
        fun newInstance(): TransactionInProgressSheet =
            TransactionInProgressSheet()
    }
}

class TransactionErrorSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_error

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! TransactionErrorSheet")
    }

    override fun initControls(view: View) {
        view.cta_button.setOnClickListener { onCtaClick() }
    }

    private fun onCtaClick() {
        dismiss()
    }

    companion object {
        fun newInstance(): TransactionErrorSheet =
            TransactionErrorSheet()
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