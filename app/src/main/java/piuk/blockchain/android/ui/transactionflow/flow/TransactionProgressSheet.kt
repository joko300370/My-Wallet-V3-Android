package piuk.blockchain.android.ui.transactionflow.flow

import android.util.DisplayMetrics
import android.view.View
import kotlinx.android.synthetic.main.dialog_tx_flow_in_progress.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionStep
import piuk.blockchain.android.ui.transactionflow.engine.TxExecutionStatus
import piuk.blockchain.android.util.maskedAsset
import timber.log.Timber

class TransactionProgressSheet(
    host: SlidingModalBottomDialog.Host
) : TransactionFlowSheet(host) {
    override val layoutResource: Int = R.layout.dialog_tx_flow_in_progress

    private val customiser: TransactionFlowCustomiser by inject()

    override fun render(newState: TransactionState) {
        Timber.d("!SEND!> Rendering! TransactionProgressSheet")
        require(newState.currentStep == TransactionStep.IN_PROGRESS)

        dialogView.tx_progress.setAssetIcon(newState.sendingAccount.asset.maskedAsset())

        when (newState.executionStatus) {
            TxExecutionStatus.IN_PROGRESS -> dialogView.tx_progress.showTxInProgress(
                customiser.transactionProgressTitle(newState),
                customiser.transactionProgressMessage(newState)
            )
            TxExecutionStatus.COMPLETED -> dialogView.tx_progress.showTxSuccess(
                customiser.transactionCompleteTitle(newState),
                customiser.transactionCompleteMessage(newState)
            )
            TxExecutionStatus.ERROR -> dialogView.tx_progress.showTxError(
                getString(R.string.send_progress_error_title),
                getString(R.string.send_progress_error_subtitle)
            )
            else -> {
            } // do nothing
        }
    }

    override fun initControls(view: View) {
        view.tx_progress.onCtaClick {
            dismiss()
        }

        // this is needed to show the expanded dialog, with space at the top and bottom
        val metrics = DisplayMetrics()
        requireActivity().windowManager?.defaultDisplay?.getMetrics(metrics)
        dialogView.layoutParams.height = (metrics.heightPixels - (48 * metrics.density)).toInt()
        dialogView.requestLayout()
    }
}