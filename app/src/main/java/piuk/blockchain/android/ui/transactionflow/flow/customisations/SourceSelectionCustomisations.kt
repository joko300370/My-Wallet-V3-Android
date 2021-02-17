package piuk.blockchain.android.ui.transactionflow.flow.customisations

import piuk.blockchain.android.ui.customviews.account.StatusDecorator
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState

interface SourceSelectionCustomisations {
    fun selectSourceAddressTitle(state: TransactionState): String
    fun selectSourceAccountTitle(state: TransactionState): String
    fun selectSourceAccountSubtitle(state: TransactionState): String
    fun selectSourceShouldShowAddNew(state: TransactionState): Boolean
    fun sourceAccountSelectionStatusDecorator(state: TransactionState): StatusDecorator
}