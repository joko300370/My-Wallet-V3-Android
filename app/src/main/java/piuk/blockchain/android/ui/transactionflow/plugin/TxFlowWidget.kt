package piuk.blockchain.android.ui.transactionflow.plugin

import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowCustomiser

interface TxFlowWidget {
    fun initControl(
        model: TransactionModel,
        customiser: TransactionFlowCustomiser,
        analytics: TxFlowAnalytics
    )

    fun update(state: TransactionState)
}
