package piuk.blockchain.android.ui.transactionflow.plugin

import io.reactivex.Observable
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.customisations.EnterAmountCustomisations

interface TxFlowWidget {

    fun initControl(
        model: TransactionModel,
        customiser: EnterAmountCustomisations,
        analytics: TxFlowAnalytics
    )

    fun update(state: TransactionState)

    fun setVisible(isVisible: Boolean)
}

interface ExpandableTxFlowWidget : TxFlowWidget {

    val expanded: Observable<Boolean>
}