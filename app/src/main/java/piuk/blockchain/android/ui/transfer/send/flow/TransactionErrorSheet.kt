package piuk.blockchain.android.ui.transfer.send.flow

import android.view.View
import kotlinx.android.synthetic.main.dialog_send_prototype.view.cta_button
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.transfer.send.SendInputSheet
import piuk.blockchain.android.ui.transfer.send.SendState
import timber.log.Timber

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
