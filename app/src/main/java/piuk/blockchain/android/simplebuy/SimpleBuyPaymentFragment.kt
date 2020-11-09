package piuk.blockchain.android.simplebuy

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.SimpleBuyAnalytics
import com.blockchain.preferences.RatingPrefs
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.ui.urllinks.URL_SUPPORT_BALANCE_LOCKED
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import info.blockchain.balance.CryptoValue
import kotlinx.android.synthetic.main.fragment_simple_buy_payment.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.cards.CardAuthoriseWebViewActivity
import piuk.blockchain.android.cards.CardVerificationFragment
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.assetName
import piuk.blockchain.android.util.extensions.secondsToDays
import piuk.blockchain.android.util.maskedAsset
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import java.math.BigInteger

class SimpleBuyPaymentFragment : MviFragment<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState>(),
    SimpleBuyScreen {

    override val model: SimpleBuyModel by scopedInject()
    private val stringUtils: StringUtils by inject()
    private val ratingPrefs: RatingPrefs by scopedInject()
    private var reviewInfo: ReviewInfo? = null
    private var isFirstLoad = false

    private val reviewManager by lazy {
        ReviewManagerFactory.create(activity)
    }

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

        // we need to make the request as soon as possible and cache the result
        if (!ratingPrefs.hasSeenRatingDialog) {
            val request = reviewManager.requestReviewFlow()
            request.addOnCompleteListener { request ->
                if (request.isSuccessful) {
                    reviewInfo = request.result
                }
            }
        }
    }

    override fun render(newState: SimpleBuyState) {
        transaction_progress_view.setAssetIcon(newState.selectedCryptoCurrency?.maskedAsset() ?: 0)

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

        if (newState.showRating) {
            tryToShowInAppRating()
        }
    }

    private fun tryToShowInAppRating() {
        reviewInfo?.let {
            val flow = reviewManager.launchReviewFlow(activity, it)
            flow.addOnCompleteListener {
                model.process(SimpleBuyIntent.AppRatingShown)
            }
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
                val lockedFundDays = lockedFundsTime.secondsToDays()
                if (lockedFundDays <= 0L) {
                    transaction_progress_view.showTxSuccess(
                        getString(R.string.card_purchased, value.formatOrSymbolForZero()),
                        getString(R.string.card_purchased_available_now,
                            getString(value.currency.assetName())))
                } else {
                    transaction_progress_view.showPendingTx(
                        getString(R.string.card_purchased, value.formatOrSymbolForZero()),
                        subtitleForLockedFunds(lockedFundDays)
                    )
                }
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

    private fun subtitleForLockedFunds(lockedFundDays: Long): SpannableStringBuilder {
        val intro =
            getString(R.string.security_locked_funds_explanation, lockedFundDays.toString())
        val map = mapOf("learn_more_link" to Uri.parse(URL_SUPPORT_BALANCE_LOCKED))

        val learnLink = stringUtils.getStringWithMappedLinks(
            R.string.common_linked_learn_more,
            map,
            activity)

        val sb = SpannableStringBuilder()
        sb.append(intro)
            .append(learnLink)
            .setSpan(
                ForegroundColorSpan(ContextCompat.getColor(activity, R.color.blue_600)),
                intro.length, intro.length + learnLink.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        return sb
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