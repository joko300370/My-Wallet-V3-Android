package piuk.blockchain.android.ui.transfer.send.flow

import android.view.View
import kotlinx.android.synthetic.main.dialog_send_confirm.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.transfer.send.SendInputSheet
import piuk.blockchain.android.ui.transfer.send.SendIntent
import piuk.blockchain.android.ui.transfer.send.SendState
import timber.log.Timber

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
