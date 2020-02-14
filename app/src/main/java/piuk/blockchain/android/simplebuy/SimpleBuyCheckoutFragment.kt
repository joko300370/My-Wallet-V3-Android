package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.notifications.analytics.SimpleBuyAnalytics
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.swap.nabu.datamanagers.Quote
import kotlinx.android.synthetic.main.fragment_simple_buy_checkout.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.ErrorSlidingBottomDialog
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import java.text.SimpleDateFormat
import java.util.Locale

class SimpleBuyCheckoutFragment : MviFragment<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState>(),
    SimpleBuyScreen,
    CancelOrderConfirmationListener {

    override val model: SimpleBuyModel by inject()
    var lastState: SimpleBuyState? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_simple_buy_checkout)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        button_buy.setOnClickListener {
            model.process(SimpleBuyIntent.ConfirmOrder)
            analytics.logEvent(SimpleBuyAnalytics.CHECKOUT_SUMMARY_CONFIRMED)
        }
        model.process(SimpleBuyIntent.FlowCurrentScreen(FlowScreen.CHECKOUT))
        activity.setupToolbar(R.string.checkout)
        analytics.logEvent(SimpleBuyAnalytics.CHECKOUT_SUMMARY_SHOWN)
        model.process(SimpleBuyIntent.FetchQuote)
        model.process(SimpleBuyIntent.FetchBankAccount)
    }

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator) ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    override fun onBackPressed(): Boolean = true

    override fun render(newState: SimpleBuyState) {
        lastState = newState
        if (newState.errorState != null) {
            showErrorState(newState.errorState)
            return
        }

        purchase_note.text = resources.getString(R.string.purchase_note, newState.selectedCryptoCurrency?.symbol)
        total_cost.text = newState.order.amount?.formatOrSymbolForZero()
        button_buy.text = resources.getString(R.string.buy_crypto, newState.selectedCryptoCurrency?.symbol)
        checkout_subtitle.text =
            resources.getString(R.string.checkout_subtitle, newState.selectedCryptoCurrency?.symbol)

        date.text = newState.order.quote?.formatDate()
        button_buy.isEnabled = newState.bankAccount != null && newState.order.quote != null

        button_cancel.setOnClickListener {
            analytics.logEvent(SimpleBuyAnalytics.CHECKOUT_SUMMARY_PRESS_CANCELL)
            showBottomSheet(SimpleBuyCancelOrderBottomSheet.newInstance(newState.selectedCryptoCurrency
                ?: return@setOnClickListener))
        }

        when (newState.order.orderState) {
            OrderState.AWAITING_FUNDS -> {
                if (newState.confirmationActionRequested)
                    navigator().goToBankDetailsScreen()
            }
            else -> {
            }
        }
    }

    private fun showErrorState(errorState: ErrorState) {
        showBottomSheet(ErrorSlidingBottomDialog.newInstance(activity))
    }

    override fun onOrderCancelationConfirmed() {
        model.process(SimpleBuyIntent.CancelOrder)
        model.process(SimpleBuyIntent.ClearState)
        navigator().exitSimpleBuyFlow()
        analytics.logEvent(SimpleBuyAnalytics.CHECKOUT_SUMMARY_CANCELLATION_CONFIRMED)
    }

    override fun onPause() {
        super.onPause()
        model.process(SimpleBuyIntent.ConfirmationHandled)
    }

    override fun onSheetClosed() {
        if (lastState?.errorState != null) {
            model.process(SimpleBuyIntent.ClearError)
            model.process(SimpleBuyIntent.ClearState)
            navigator().exitSimpleBuyFlow()
        }
    }
}

private fun Quote.formatDate(): String {
    val format = SimpleDateFormat("MMMM d, yyyy @ hh:mm aa", Locale.getDefault())
    return format.format(this.date)
}

interface CancelOrderConfirmationListener {
    fun onOrderCancelationConfirmed()
}