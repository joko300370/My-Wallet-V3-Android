package piuk.blockchain.android.ui.transfer.send.flow

import android.view.View
import kotlinx.android.synthetic.main.dialog_send_in_progress.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.transfer.send.SendInputSheet
import piuk.blockchain.android.ui.transfer.send.SendState
import piuk.blockchain.android.ui.transfer.send.SendStep
import piuk.blockchain.android.ui.transfer.send.TransactionInFlightState
import piuk.blockchain.android.util.maskedAsset
import timber.log.Timber

class TransactionProgressSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_in_progress

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! TransactionInProgressSheet")
        require(newState.currentStep == SendStep.IN_PROGRESS)

        dialogView.send_tx_progress.setAssetIcon(newState.sendingAccount.asset.maskedAsset())
        when (newState.transactionInFlight) {
            TransactionInFlightState.IN_PROGRESS -> dialogView.send_tx_progress.showTxInProgress(
                "progressing", "please wait"
            )
            TransactionInFlightState.COMPLETED -> dialogView.send_tx_progress.showTxSuccess(
                "Success!", "all good to go"
            )
            TransactionInFlightState.ERROR -> dialogView.send_tx_progress.showTxSuccess(
                "something went wrong!", "not to worry though"
            )
            else -> {} // do nothing
        }
    }

    override fun initControls(view: View) {
        dialogView.send_tx_progress.onCtaClick {
            dismiss()
        }
    }

    companion object {
        fun newInstance(): TransactionProgressSheet =
            TransactionProgressSheet()
    }
}