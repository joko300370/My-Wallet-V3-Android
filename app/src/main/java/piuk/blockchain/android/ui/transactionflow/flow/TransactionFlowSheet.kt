package piuk.blockchain.android.ui.transactionflow.flow

import android.content.DialogInterface
import androidx.annotation.StringRes
import org.koin.android.ext.android.inject
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet
import piuk.blockchain.android.ui.transactionflow.transactionInject
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

abstract class TransactionFlowSheet(
    override val host: SlidingModalBottomDialog.Host
) : MviBottomSheet<TransactionModel, TransactionIntent, TransactionState>() {

    override val model: TransactionModel by transactionInject()

    protected var state: TransactionState = TransactionState()
        private set

    protected val analyticsHooks: TxFlowAnalytics by inject()

    protected fun showErrorToast(@StringRes msgId: Int) {
        ToastCustom.makeText(
            activity,
            getString(msgId),
            ToastCustom.LENGTH_LONG,
            ToastCustom.TYPE_ERROR
        )
    }

    override fun onCancel(dialog: DialogInterface) {
        analyticsHooks.onFlowCanceled(state)
        super.onCancel(dialog)
    }

    protected fun cacheState(newState: TransactionState) {
        state = newState
    }
}