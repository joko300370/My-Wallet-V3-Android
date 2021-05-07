package piuk.blockchain.android.ui.transactionflow.flow.customisations

import android.content.Context
import android.widget.FrameLayout
import piuk.blockchain.android.ui.customviews.CurrencyType
import piuk.blockchain.android.ui.customviews.account.StatusDecorator
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.plugin.TxFlowWidget

interface TargetSelectionCustomisations {
    fun selectTargetAddressTitle(state: TransactionState): String
    fun selectTargetAddressInputHint(state: TransactionState): String
    fun selectTargetNoAddressMessageText(state: TransactionState): String?
    fun selectTargetShowManualEnterAddress(state: TransactionState): Boolean
    fun selectTargetShouldShowSubtitle(state: TransactionState): Boolean
    fun selectTargetSubtitle(state: TransactionState): String
    fun shouldShowCustodialUpsell(state: TransactionState): Boolean
    fun selectTargetAddressWalletsCta(state: TransactionState): String
    fun selectTargetSourceLabel(state: TransactionState): String
    fun selectTargetDestinationLabel(state: TransactionState): String
    fun selectTargetStatusDecorator(state: TransactionState): StatusDecorator
    fun selectTargetAccountTitle(state: TransactionState): String
    fun selectTargetAccountDescription(state: TransactionState): String
    fun enterTargetAddressSheetState(state: TransactionState): TargetAddressSheetState
    fun issueFlashMessage(state: TransactionState, input: CurrencyType?): String?
    fun installAddressSheetSource(ctx: Context, frame: FrameLayout, state: TransactionState): TxFlowWidget
}