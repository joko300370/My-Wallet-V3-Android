package piuk.blockchain.android.ui.transfer.send.flow

import android.view.View
import kotlinx.android.synthetic.main.dialog_send_in_progress.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.transfer.send.SendInputSheet
import piuk.blockchain.android.ui.transfer.send.SendIntent
import piuk.blockchain.android.ui.transfer.send.SendState
import piuk.blockchain.android.ui.transfer.send.SendStep
import piuk.blockchain.android.ui.transfer.send.TransactionInFlightState
import piuk.blockchain.android.util.maskedAsset
import timber.log.Timber

class TransactionProgressSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_in_progress

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! TransactionProgressSheet")
        require(newState.currentStep == SendStep.IN_PROGRESS)

        dialogView.send_tx_progress.setAssetIcon(newState.sendingAccount.asset.maskedAsset())

        when (newState.transactionInFlight) {
            TransactionInFlightState.IN_PROGRESS -> dialogView.send_tx_progress.showTxInProgress(
                getString(R.string.send_progress_sending_title),
                getString(R.string.send_progress_sending_subtitle)
            )
            TransactionInFlightState.COMPLETED -> dialogView.send_tx_progress.showTxSuccess(
                getString(R.string.send_progress_complete_title),
                getString(R.string.send_progress_complete_subtitle)
            )
            TransactionInFlightState.ERROR -> dialogView.send_tx_progress.showTxError(
                getString(R.string.send_progress_error_title),
                getString(R.string.send_progress_error_subtitle)
            )
            else -> {} // do nothing
        }
    }

    override fun initControls(view: View) {
        view.send_tx_progress.onCtaClick {
            dismiss()
        }

        view.next.setOnClickListener {
            model.process(SendIntent.ToState(TransactionInFlightState.COMPLETED))
        }
        view.next1.setOnClickListener {
            model.process(SendIntent.ToState(TransactionInFlightState.ERROR))
        }

        view.previous.setOnClickListener {
            model.process(SendIntent.ToState(TransactionInFlightState.IN_PROGRESS))
        }
    }

    companion object {
        fun newInstance(): TransactionProgressSheet =
            TransactionProgressSheet()
    }
}