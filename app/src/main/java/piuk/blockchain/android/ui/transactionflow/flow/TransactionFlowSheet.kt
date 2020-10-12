package piuk.blockchain.android.ui.transactionflow.flow

import androidx.annotation.StringRes
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet
import piuk.blockchain.android.ui.transactionflow.transactionInject
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

abstract class TransactionFlowSheet(
    override val host: SlidingModalBottomDialog.Host
) : MviBottomSheet<TransactionModel, TransactionIntent, TransactionState>() {

    override val model: TransactionModel by transactionInject()

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