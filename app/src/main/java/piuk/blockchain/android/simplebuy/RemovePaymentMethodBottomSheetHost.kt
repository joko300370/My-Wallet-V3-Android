package piuk.blockchain.android.simplebuy

import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

interface RemovePaymentMethodBottomSheetHost : SlidingModalBottomDialog.Host {
    fun onCardRemoved(cardId: String)
    fun onLinkedBankRemoved(bankId: String)
}