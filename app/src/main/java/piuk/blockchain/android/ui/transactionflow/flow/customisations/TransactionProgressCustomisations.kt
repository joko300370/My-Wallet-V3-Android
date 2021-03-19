package piuk.blockchain.android.ui.transactionflow.flow.customisations

import piuk.blockchain.android.ui.transactionflow.engine.TransactionState

interface TransactionProgressCustomisations {
    fun transactionProgressTitle(state: TransactionState): String
    fun transactionProgressMessage(state: TransactionState): String
    fun transactionCompleteTitle(state: TransactionState): String
    fun transactionCompleteMessage(state: TransactionState): String
    fun transactionProgressIcon(state: TransactionState): Int
    fun transactionProgressExceptionMessage(state: TransactionState): String
}