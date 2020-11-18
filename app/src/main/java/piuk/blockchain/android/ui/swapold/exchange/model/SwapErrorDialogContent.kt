package piuk.blockchain.android.ui.swapold.exchange.model

import piuk.blockchain.android.ui.customviews.ErrorBottomDialog

data class SwapErrorDialogContent(
    val content: ErrorBottomDialog.Content,
    val ctaClick: (() -> Unit)?,
    val dismissClick: (() -> Unit)?
)