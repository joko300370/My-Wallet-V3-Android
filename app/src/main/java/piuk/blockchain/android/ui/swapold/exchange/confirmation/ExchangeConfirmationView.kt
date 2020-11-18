package piuk.blockchain.android.ui.swapold.exchange.confirmation

import androidx.annotation.StringRes
import com.blockchain.swap.common.exchange.mvi.ExchangeViewState
import info.blockchain.balance.CryptoValue
import io.reactivex.Observable
import piuk.blockchain.android.ui.swapold.exchange.model.SwapErrorDialogContent
import piuk.blockchain.android.ui.swapold.exchange.model.Trade
import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

interface ExchangeConfirmationView : View {

    val exchangeViewState: Observable<ExchangeViewState>

    fun onTradeSubmitted(trade: Trade, firstGoldPaxTrade: Boolean)

    fun showSecondPasswordDialog()

    fun showProgressDialog()

    fun dismissProgressDialog()

    fun displayErrorDialog(@StringRes message: Int)

    fun displayErrorBottomDialog(swapErrorDialogContent: SwapErrorDialogContent)

    fun updateFee(cryptoValue: CryptoValue)

    fun goBack()

    fun openTiersCard()

    fun openMoreInfoLink(link: String)

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType type: String)
}
