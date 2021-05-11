package piuk.blockchain.android.ui.transactionflow.flow.customisations

import android.content.Context
import android.widget.FrameLayout
import piuk.blockchain.android.ui.customviews.CurrencyType
import piuk.blockchain.android.ui.transactionflow.engine.TransactionErrorState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.plugin.EnterAmountWidget

interface EnterAmountCustomisations {
    fun enterAmountTitle(state: TransactionState): String
    fun enterAmountActionIcon(state: TransactionState): Int
    fun enterAmountActionIconCustomisation(state: TransactionState): Boolean
    fun enterAmountMaxButton(state: TransactionState): String
    fun enterAmountSourceLabel(state: TransactionState): String
    fun enterAmountTargetLabel(state: TransactionState): String
    fun enterAmountSourceIcon(state: TransactionState): Int
    fun defInputType(state: TransactionState, fiatCurrency: String): CurrencyType
    fun selectIssueType(state: TransactionState): IssueType
    fun showTargetIcon(state: TransactionState): Boolean
    fun shouldDisableInput(errorState: TransactionErrorState): Boolean
    fun issueFlashMessage(state: TransactionState, input: CurrencyType?): String?
    fun issueFeesTooHighMessage(state: TransactionState): String?
    fun installEnterAmountLowerSlotView(ctx: Context, frame: FrameLayout, state: TransactionState): EnterAmountWidget
    fun installEnterAmountUpperSlotView(ctx: Context, frame: FrameLayout, state: TransactionState): EnterAmountWidget
    fun shouldShowMaxLimit(state: TransactionState): Boolean
    fun enterAmountLimitsViewTitle(state: TransactionState): String
    fun enterAmountLimitsViewInfo(state: TransactionState): String
    fun enterAmountMaxNetworkFeeLabel(state: TransactionState): String
}