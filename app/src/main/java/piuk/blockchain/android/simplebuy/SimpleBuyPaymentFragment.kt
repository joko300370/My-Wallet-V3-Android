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
import piuk.blockchain.android.R
import piuk.blockchain.android.cards.CardAuthoriseWebViewActivity
import piuk.blockchain.android.cards.CardVerificationFragment
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.android.util.assetName
import piuk.blockchain.android.util.maskedAsset
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import java.math.BigInteger

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
        transaction_progress_view.setAssetIcon(newState.selectedCryptoCurrency?.maskedAsset() ?: -1)

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
                newState.paymentPending,
                newState.withdrawalLockPeriod
            )
        } ?: renderTitleAndSubtitle(
            null,
            newState.isLoading,
            false,
            newState.errorState != null,
            false
        )

        transaction_progress_view.onCtaClick {
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
        }
    }

    private fun renderTitleAndSubtitle(
        value: CryptoValue?,
        loading: Boolean,
        paymentSucceeded: Boolean,
        hasError: Boolean,
        pending: Boolean,
        lockedFundsTime: BigInteger = BigInteger.ZERO
    ) {
        when {
            paymentSucceeded && value != null -> {
                transaction_progress_view.showTxSuccess(
                    getString(R.string.card_purchased, value.formatOrSymbolForZero()),
                    getString(R.string.card_purchased_available_now,
                        getString(value.currency.assetName())),
                    lockedFundsTime,
                    requireActivity())
            }
            loading && value != null -> {
                transaction_progress_view.showTxInProgress(
                    getString(R.string.card_buying, value.formatOrSymbolForZero()),
                    getString(R.string.completing_card_buy))
            }
            pending && value != null -> {
                transaction_progress_view.showTxPending(
                    getString(R.string.card_in_progress, value.formatOrSymbolForZero()),
                    getString(R.string.we_will_notify_order_complete))
            }
            hasError -> {
                transaction_progress_view.showTxError(
                    getString(R.string.card_error_title),
                    getString(R.string.order_error_subtitle))
            }
        }
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
        (activity as? SimpleBuyNavigator) ?: throw IllegalStateException(
            "Parent must implement SimpleBuyNavigator")

    override fun onBackPressed(): Boolean = true

    override fun backPressedHandled(): Boolean {
        return true
    }
}