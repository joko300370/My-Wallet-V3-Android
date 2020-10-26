package piuk.blockchain.android.ui.swapold.exchange.history

import piuk.blockchain.androidcoreui.ui.base.View

interface TradeHistoryView : View {

    fun renderUi(uiState: ExchangeUiState)
}