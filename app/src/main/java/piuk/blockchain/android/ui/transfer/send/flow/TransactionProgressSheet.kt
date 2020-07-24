package piuk.blockchain.android.ui.transfer.send.flow

import android.util.DisplayMetrics
import android.view.View
import kotlinx.android.synthetic.main.dialog_send_in_progress.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.transfer.send.SendInputSheet
import piuk.blockchain.android.ui.transfer.send.SendIntent
import piuk.blockchain.android.ui.transfer.send.SendState
import piuk.blockchain.android.ui.transfer.send.SendStep
import piuk.blockchain.android.ui.transfer.send.TransactionInFlightState
import piuk.blockchain.android.util.assetName
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
                getString(R.string.send_progress_sending_title,
                    newState.sendAmount.toStringWithSymbol()),
                getString(R.string.send_progress_sending_subtitle)
            )
            TransactionInFlightState.COMPLETED -> dialogView.send_tx_progress.showTxSuccess(
                getString(R.string.send_progress_complete_title,
                    newState.sendAmount.toStringWithSymbol()),
                getString(R.string.send_progress_complete_subtitle,
                    getString(newState.sendingAccount.asset.assetName()))
            )
            TransactionInFlightState.ERROR -> dialogView.send_tx_progress.showTxError(
                getString(R.string.send_progress_error_title),
                getString(R.string.send_progress_error_subtitle)
            )
            else -> {
            } // do nothing
        }
    }

    override fun initControls(view: View) {
        view.send_tx_progress.onCtaClick {
            dismiss()
        }

        // this is needed to show the expanded dialog, with space at the top and bottom
        val metrics = DisplayMetrics()
        requireActivity().windowManager?.defaultDisplay?.getMetrics(metrics)
        dialogView.layoutParams.height = (metrics.heightPixels - (48 * metrics.density)).toInt()
        dialogView.requestLayout()
    }

    companion object {
        fun newInstance(): TransactionProgressSheet = TransactionProgressSheet()
    }
}