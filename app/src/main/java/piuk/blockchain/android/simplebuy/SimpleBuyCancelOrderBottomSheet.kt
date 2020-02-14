package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.view.View
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.SimpleBuyAnalytics
import info.blockchain.balance.CryptoCurrency
import kotlinx.android.synthetic.main.simple_buy_cancel_order_bottom_sheet.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import java.lang.IllegalStateException

class SimpleBuyCancelOrderBottomSheet : SlidingModalBottomDialog() {

    private val cryptoCurrency: CryptoCurrency by unsafeLazy {
        arguments?.getSerializable(SELECTED_CRYPTOCURRENCY_KEY) as? CryptoCurrency
            ?: throw IllegalStateException("No cryptocurrency passed")
    }
    private val analytics: Analytics by inject()

    override val layoutResource: Int = R.layout.simple_buy_cancel_order_bottom_sheet

    override fun initControls(view: View) {
        with(view) {
            cancel_order_token.text = getString(R.string.cancel_token_instruction, cryptoCurrency.symbol)
            cancel_order.setOnClickListener {
                (parentFragment as? CancelOrderConfirmationListener)?.onOrderCancelationConfirmed()
                dismiss()
            }
            go_back.setOnClickListener {
                dismiss()
                analytics.logEvent(SimpleBuyAnalytics.CHECKOUT_SUMMARY_CANCELLATION_GO_BACK)
            }
        }
    }

    companion object {
        private const val SELECTED_CRYPTOCURRENCY_KEY = "selected_cryptocurrency"
        fun newInstance(cryptoCurrency: CryptoCurrency): SimpleBuyCancelOrderBottomSheet {
            val bundle = Bundle()
            bundle.putSerializable(SELECTED_CRYPTOCURRENCY_KEY, cryptoCurrency)
            return SimpleBuyCancelOrderBottomSheet().apply {
                arguments = bundle
            }
        }
    }
}