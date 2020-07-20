package piuk.blockchain.android.simplebuy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.SimpleBuyAnalytics
import com.blockchain.swap.nabu.datamanagers.OrderState
import info.blockchain.balance.CryptoValue
import kotlinx.android.synthetic.main.fragment_simple_buy_payment.*
import kotlinx.android.synthetic.main.fragment_simple_buy_payment.icon
import piuk.blockchain.android.R
import piuk.blockchain.android.cards.CardAuthoriseWebViewActivity
import piuk.blockchain.android.cards.CardVerificationFragment
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.android.util.assetName
import piuk.blockchain.android.util.maskedAsset
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visibleIf

class SimpleBuyPaymentFragment : MviFragment<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState>(),
    SimpleBuyScreen {
    override val model: SimpleBuyModel by scopedInject()

    private var isFirstLoad = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isFirstLoad = savedInstanceState == null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_simple_buy_payment)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.setupToolbar(R.string.payment, false)
    }

    override fun render(newState: SimpleBuyState) {

        icon.setImageResource(newState.selectedCryptoCurrency?.maskedAsset() ?: -1)
        if (newState.orderState == OrderState.AWAITING_FUNDS && isFirstLoad) {
            model.process(SimpleBuyIntent.MakePayment(newState.id ?: return))
            isFirstLoad = false
        }

        newState.orderValue?.let {
            renderTitleAndSubtitle(
                it,
                newState.isLoading,
                newState.paymentSucceeded,
                newState.errorState != null,
                newState.paymentPending
            )
        } ?: renderTitleAndSubtitle(
            null,
            newState.isLoading,
            false,
            newState.errorState != null,
            false
        )

        ok_btn.setOnClickListener {
            if (!newState.paymentPending)
                navigator().exitSimpleBuyFlow()
            else
                navigator().goToPendingOrderScreen()
        }

        newState.everypayAuthOptions?.let {
            openWebView(
                newState.everypayAuthOptions.paymentLink,
                newState.everypayAuthOptions.exitLink
            )
            progress.visibility = View.GONE
        }
    }

    private fun renderTitleAndSubtitle(
        value: CryptoValue?,
        loading: Boolean,
        paymentSucceeded: Boolean,
        hasError: Boolean,
        pending: Boolean
    ) {
        when {
            paymentSucceeded && value != null -> {
                title.text = getString(R.string.card_purchased, value.formatOrSymbolForZero())
                subtitle.text = getString(R.string.card_purchased_available_now, getString(value.currency.assetName()))
            }
            loading && value != null -> {
                title.text = getString(R.string.card_buying, value.formatOrSymbolForZero())
                subtitle.text = getString(R.string.completing_card_buy)
            }
            pending && value != null -> {
                title.text = getString(R.string.card_in_progress, value.formatOrSymbolForZero())
                subtitle.text = getString(R.string.we_will_notify_order_complete)
            }
            hasError -> {
                icon.setImageResource(R.drawable.ic_alert)
                title.text = getString(R.string.card_error_title)
                subtitle.text = getString(R.string.order_error_subtitle)
            }
        }

        state_indicator.visibleIf { paymentSucceeded || pending }
        progress.visibleIf { loading }
        ok_btn.visibleIf { paymentSucceeded || pending || hasError }
        state_indicator.setImageResource(if (pending) R.drawable.ic_pending_clock else R.drawable.ic_check_circle)
    }

    private fun openWebView(paymentLink: String, exitLink: String) {
        CardAuthoriseWebViewActivity.start(fragment = this, link = paymentLink, exitLink = exitLink)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CardVerificationFragment.EVERYPAY_AUTH_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                model.process(SimpleBuyIntent.CheckOrderStatus)
                analytics.logEvent(SimpleBuyAnalytics.CARD_3DS_COMPLETED)
            } else {
                model.process(SimpleBuyIntent.ErrorIntent())
            }
        }
    }

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator) ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    override fun onBackPressed(): Boolean = true

    override fun backPressedHandled(): Boolean {
        return true
    }
}