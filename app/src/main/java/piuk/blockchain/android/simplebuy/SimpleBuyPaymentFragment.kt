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
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.preferences.RatingPrefs
import com.blockchain.ui.urllinks.URL_SUPPORT_BALANCE_LOCKED
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.cards.CardAuthoriseWebViewActivity
import piuk.blockchain.android.cards.CardVerificationFragment
import piuk.blockchain.android.databinding.FragmentSimpleBuyPaymentBinding
import piuk.blockchain.android.sdd.SDDAnalytics
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionFlowCustomiserImpl.Companion.getEstimatedTransactionCompletionTime
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.assetName
import piuk.blockchain.android.util.maskedAsset
import piuk.blockchain.android.util.secondsToDays

class SimpleBuyPaymentFragment : MviFragment<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState>(),
    SimpleBuyScreen, UnlockHigherLimitsBottomSheet.Host {

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

    private var _binding: FragmentSimpleBuyPaymentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSimpleBuyPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun render(newState: SimpleBuyState) {
        binding.transactionProgressView.setAssetIcon(newState.selectedCryptoCurrency?.maskedAsset() ?: 0)

        if (newState.orderState == OrderState.AWAITING_FUNDS && isFirstLoad) {
            model.process(SimpleBuyIntent.MakePayment(newState.id ?: return))
            isFirstLoad = false
        }
        require(newState.selectedPaymentMethod != null)

        renderTitleAndSubtitle(
            newState
        )

        binding.transactionProgressView.onCtaClick {
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

    private fun renderTitleAndSubtitle(newState: SimpleBuyState) {
        require(newState.selectedPaymentMethod != null)
        when {
            newState.paymentSucceeded && newState.orderValue != null -> {
                val lockedFundDays = newState.withdrawalLockPeriod.secondsToDays()
                if (lockedFundDays <= 0L) {
                    binding.transactionProgressView.showTxSuccess(
                        getString(R.string.card_purchased, newState.orderValue.formatOrSymbolForZero()),
                        getString(
                            R.string.card_purchased_available_now,
                            getString(newState.orderValue.currency.assetName())
                        )
                    )
                } else {
                    binding.transactionProgressView.showPendingTx(
                        getString(R.string.card_purchased, newState.orderValue.formatOrSymbolForZero()),
                        subtitleForLockedFunds(
                            lockedFundDays, newState.selectedPaymentMethod.paymentMethodType
                        )
                    )
                }
                checkForUnlockHigherLimits(newState.shouldShowUnlockHigherFunds)
            }
            newState.isLoading && newState.orderValue != null -> {
                binding.transactionProgressView.showTxInProgress(
                    getString(R.string.card_buying, newState.orderValue.formatOrSymbolForZero()),
                    getString(R.string.completing_card_buy)
                )
            }
            newState.paymentPending && newState.orderValue != null -> {
                when (newState.selectedPaymentMethod.paymentMethodType) {
                    PaymentMethodType.BANK_TRANSFER -> {
                        binding.transactionProgressView.showTxPending(
                            getString(
                                R.string.bank_transfer_in_progress_title, newState.orderValue.formatOrSymbolForZero()
                            ),
                            getString(R.string.bank_transfer_in_progress_blurb, getEstimatedTransactionCompletionTime())
                        )
                    }
                    else -> {
                        binding.transactionProgressView.showTxPending(
                            getString(R.string.card_in_progress, newState.orderValue.formatOrSymbolForZero()),
                            getString(R.string.we_will_notify_order_complete)
                        )
                    }
                }
            }
            newState.errorState != null -> {
                binding.transactionProgressView.showTxError(
                    getString(R.string.common_oops),
                    getString(R.string.order_error_subtitle)
                )
            }
        }
    }

    private fun checkForUnlockHigherLimits(shouldShowUnlockMoreFunds: Boolean) {
        if (!shouldShowUnlockMoreFunds)
            return
        binding.transactionProgressView.configureSecondaryButton(getString(R.string.want_to_buy_more)) {
            showBottomSheet(UnlockHigherLimitsBottomSheet())
        }
    }

    private fun subtitleForLockedFunds(lockedFundDays: Long, paymentMethod: PaymentMethodType): SpannableStringBuilder {
        val intro = when (paymentMethod) {
            PaymentMethodType.PAYMENT_CARD -> getString(
                R.string.security_locked_card_funds_explanation,
                lockedFundDays.toString()
            )
            PaymentMethodType.BANK_TRANSFER ->
                getString(
                    R.string.security_locked_funds_bank_transfer_payment_screen_explanation,
                    lockedFundDays.toString()
                )
            else -> return SpannableStringBuilder()
        }

        val map = mapOf("learn_more_link" to Uri.parse(URL_SUPPORT_BALANCE_LOCKED))

        val learnLink = stringUtils.getStringWithMappedAnnotations(
            R.string.common_linked_learn_more,
            map,
            activity
        )

        val sb = SpannableStringBuilder()
        sb.append(intro)
            .append(learnLink)
            .setSpan(
                ForegroundColorSpan(ContextCompat.getColor(activity, R.color.blue_600)),
                intro.length, intro.length + learnLink.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

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
        if (requestCode == SimpleBuyActivity.KYC_STARTED &&
            resultCode == SimpleBuyActivity.RESULT_KYC_SIMPLE_BUY_COMPLETE
        ) {
            navigator().exitSimpleBuyFlow()
        }
    }

    override fun unlockHigherLimits() {
        KycNavHostActivity.startForResult(this, CampaignType.SimpleBuy, SimpleBuyActivity.KYC_STARTED)
        analytics.logEvent(SDDAnalytics.UPGRADE_TO_GOLD_CLICKED)
    }

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator) ?: throw IllegalStateException(
            "Parent must implement SimpleBuyNavigator"
        )

    override fun onBackPressed(): Boolean = true

    override fun backPressedHandled(): Boolean {
        return true
    }
}