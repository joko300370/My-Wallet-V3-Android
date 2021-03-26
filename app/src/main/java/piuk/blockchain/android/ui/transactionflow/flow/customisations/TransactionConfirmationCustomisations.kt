package piuk.blockchain.android.ui.transactionflow.flow.customisations

import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState

interface TransactionConfirmationCustomisations {
    fun confirmTitle(state: TransactionState): String
    fun confirmCtaText(state: TransactionState): String
    fun confirmListItemTitle(assetAction: AssetAction): String
    fun confirmDisclaimerBlurb(assetAction: AssetAction): String
    fun confirmDisclaimerVisibility(assetAction: AssetAction): Boolean
    fun amountHeaderConfirmationVisible(state: TransactionState): Boolean
}