package piuk.blockchain.android.ui.transactionflow.plugin

import io.reactivex.Observable
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.customisations.EnterAmountCustomisations
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionConfirmationCustomisations

interface EnterAmountWidget : TxFlowWidget {
    fun initControl(
        model: TransactionModel,
        customiser: EnterAmountCustomisations,
        analytics: TxFlowAnalytics
    )
}

interface ConfirmSheetWidget : TxFlowWidget {
    fun initControl(
        model: TransactionModel,
        customiser: TransactionConfirmationCustomisations,
        analytics: TxFlowAnalytics
    )
}

interface TxFlowWidget {
    fun update(state: TransactionState)

    fun setVisible(isVisible: Boolean)
}

interface ExpandableTxFlowWidget : EnterAmountWidget {

    val expanded: Observable<Boolean>
}