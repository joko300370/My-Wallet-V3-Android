package piuk.blockchain.android.ui.kyc.tiersplash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.navigation.NavDirections
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.KYCAnalyticsEvents
import com.blockchain.notifications.analytics.kycTierStart
import com.blockchain.notifications.analytics.logEvent
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.models.responses.nabu.KycTierState
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.models.responses.nabu.Tier
import com.blockchain.ui.extensions.throttledClicks
import com.blockchain.ui.urllinks.URL_CONTACT_SUPPORT
import com.blockchain.ui.urllinks.URL_LEARN_MORE_REJECTED
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.databinding.FragmentKycSplashBinding
import piuk.blockchain.android.databinding.FragmentKycTierSplashBinding
import piuk.blockchain.android.ui.kyc.hyperlinks.renderSingleLink
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.android.ui.kyc.navigate
import piuk.blockchain.android.ui.transactionflow.DialogFlow
import piuk.blockchain.android.ui.transactionflow.TransactionFlow
import piuk.blockchain.android.util.setImageDrawable
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.toast
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.inflate
import piuk.blockchain.android.util.visible
import timber.log.Timber

class KycTierSplashFragment : BaseFragment<KycTierSplashView, KycTierSplashPresenter>(),
    KycTierSplashView, DialogFlow.FlowHost {

    private val presenter: KycTierSplashPresenter by scopedInject()
    private val analytics: Analytics by inject()
    private val progressListener: KycProgressListener by ParentActivityDelegate(
        this
    )

    private var _binding: FragmentKycTierSplashBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKycTierSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logEvent(AnalyticsEvents.KycTiers)

        val showContent = arguments?.getBoolean(KycNavHostActivity.EXTRA_SHOW_TIERS_LIMITS_SPLASH) ?: false

        val title = when (progressListener.campaignType) {
            CampaignType.Swap -> R.string.kyc_splash_title
            CampaignType.Sunriver,
            CampaignType.SimpleBuy,
            CampaignType.Blockstack,
            CampaignType.Resubmission,
            CampaignType.None,
            CampaignType.FiatFunds,
            CampaignType.Interest -> R.string.identity_verification
        }
        binding.container.visibility = if (showContent) View.VISIBLE else View.GONE
        progressListener.setHostTitle(title)

        binding.textViewEligible.renderSingleLink(
            R.string.by_completing_gold_level_you_will_be_eligible_to_participate_in_our_airdrop_program,
            R.string.learn_more,
            R.string.airdrop_learn_more_url
        )

        onViewReady()
    }

    private val disposable = CompositeDisposable()

    override fun renderTiersList(tiers: KycTiers) {
        // Logic is now limited to 2 tiers, future refactor to traverse tiersList

        renderTier1(tiers.tierForLevel(KycTierLevel.SILVER))
        renderTier2(tiers.tierForLevel(KycTierLevel.GOLD))

        reportState(
            tiers.tierForLevel(KycTierLevel.SILVER).state,
            tiers.tierForLevel(KycTierLevel.GOLD).state
        )
    }

    private fun reportState(
        state1: KycTierState,
        state2: KycTierState
    ) {
        val pendingOrApproved = listOf(KycTierState.Pending, KycTierState.Verified)
        when {
            state2 in pendingOrApproved -> logEvent(AnalyticsEvents.KycTier2Complete)
            state1 in pendingOrApproved -> logEvent(AnalyticsEvents.KycTier1Complete)
            state1 == KycTierState.None -> logEvent(AnalyticsEvents.KycTiersLocked)
        }
    }

    private fun renderTier(tier: Tier, layoutElements: TierLayoutElements) {
        with(binding) {
            when (tier.state) {
                KycTierState.Rejected -> {
                    layoutElements.icon.setImageDrawable(R.drawable.vector_tier_locked)
                    textHeaderTiersLine1.text = getString(R.string.swap_unavailable)
                    textHeaderTiersLine2.text = getString(R.string.swap_unavailable_explained)
                    layoutElements.cardTier.alpha = 0.2F
                    textContactSupport.visible()
                    buttonLearnMore.visible()
                    buttonSwapNow.gone()
                }
                KycTierState.Pending -> {
                    layoutElements.icon.setImageDrawable(R.drawable.vector_tier_review)
                    layoutElements.textTierState.visible()
                    layoutElements.textTierState.text = getString(R.string.in_review)
                    layoutElements.textTierState.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.kyc_in_progress
                        )
                    )
                    textHeaderTiersLine2.text = getString(R.string.tier_x_in_review, getLevelForTier(tier))
                    buttonLearnMore.gone()
                    textContactSupport.gone()
                }
                KycTierState.Verified -> {
                    layoutElements.icon.setImageDrawable(R.drawable.vector_tier_verified)
                    layoutElements.textTierState.visible()
                    layoutElements.textTierState.text = getString(R.string.approved)
                    tierAvailableFiat.text = getLimitForTier(tier)
                    tierAvailableFiat.visible()
                    textHeaderTiersLine1.text = getString(R.string.available)
                    textHeaderTiersLine2.text = getString(R.string.swap_limit)
                    buttonSwapNow.visible()
                }
                else -> {
                    layoutElements.textTierRequires.visible()
                    layoutElements.icon.setImageDrawable(R.drawable.vector_tier_start)
                    buttonLearnMore.gone()
                    textContactSupport.gone()
                }
            }
            layoutElements.textLimit.text = getLimitForTier(tier)
            layoutElements.textPeriodicLimit.text = getString(getLimitString(tier))
        }
    }

    private fun renderTier1(tier: Tier) {
        with(binding) {
            val layoutElements = TierLayoutElements(
                cardTier = cardTier1,
                icon = iconTier1State,
                textLevel = textTier1Level,
                textLimit = textTier1Limit,
                textPeriodicLimit = textTier1PeriodicLimit,
                textTierState = textTier1State,
                textTierRequires = textTier1Requires
            )
            renderTier(tier, layoutElements)
        }
    }

    private fun renderTier2(tier: Tier) {
        with(binding) {
            val layoutElements = TierLayoutElements(
                cardTier = cardTier2,
                icon = iconTier2State,
                textLevel = textTier2Level,
                textLimit = textTier2Limit,
                textPeriodicLimit = textTier2PeriodicLimit,
                textTierState = textTier2State,
                textTierRequires = textTier2Requires
            )
            renderTier(tier, layoutElements)
        }
    }

    private fun getLevelForTier(tier: Tier): String =
        when (tier.state.ordinal) {
            1 -> getString(R.string.silver_level)
            2 -> getString(R.string.gold_level)
            else -> ""
        }

    private fun getLimitForTier(tier: Tier): String? {
        val limits = tier.limits
        return (limits?.annualFiat ?: limits?.dailyFiat)?.toStringWithSymbol() ?: ""
    }

    @StringRes
    private fun getLimitString(tier: Tier): Int {
        val limits = tier.limits
        return when {
            limits?.annualFiat != null -> if (tier.state.ordinal == SILVER_TIER_INDEX)
                R.string.annual_swap_limit else R.string.annual_swap_and_buy_limit
            limits?.dailyFiat != null -> if (tier.state.ordinal == SILVER_TIER_INDEX)
                R.string.daily_swap_limit else R.string.daily_swap_and_buy_limit
            else -> R.string.generic_limit
        }
    }

    override fun onResume() {
        super.onResume()
        disposable += binding.cardTier1
            .throttledClicks()
            .subscribeBy(
                onNext = {
                    analytics.logEvent(KYCAnalyticsEvents.Tier1Clicked)
                    presenter.tier1Selected()
                },
                onError = { Timber.e(it) }
            )
        disposable += binding.cardTier2
            .throttledClicks()
            .subscribeBy(
                onNext = {
                    analytics.logEvent(KYCAnalyticsEvents.Tier2Clicked)
                    presenter.tier2Selected()
                },
                onError = { Timber.e(it) }
            )
        disposable += binding.buttonSwapNow
                .throttledClicks()
                .subscribeBy(
                    onNext = {
                        startSwap()
                    },
                    onError = { Timber.e(it) }
                )
        disposable += binding.buttonLearnMore
                .throttledClicks()
                .subscribeBy(
                    onNext = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_LEARN_MORE_REJECTED))) },
                    onError = { Timber.e(it) }
                )
        disposable += binding.textContactSupport
                .throttledClicks()
                .subscribeBy(
                    onNext = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_CONTACT_SUPPORT))) },
                    onError = { Timber.e(it) }
                )
    }

    private fun startSwap() {
        TransactionFlow(
            action = AssetAction.Swap
        ).apply {
            startFlow(
                fragmentManager = childFragmentManager,
                host = this@KycTierSplashFragment
            )
        }
    }

    override fun onPause() {
        disposable.clear()
        super.onPause()
    }

    override fun createPresenter() = presenter

    override fun getMvpView() = this

    override fun navigateTo(directions: NavDirections, tier: Int) {
        logEvent(kycTierStart(tier))
        navigate(directions)
    }

    override fun showError(message: Int) =
        toast(message, ToastCustom.TYPE_ERROR)

    private inner class TierLayoutElements(
        val cardTier: CardView,
        val icon: ImageView,
        val textLevel: TextView,
        val textLimit: TextView,
        val textPeriodicLimit: TextView,
        val textTierState: TextView,
        val textTierRequires: TextView
    )

    companion object {
        private const val SILVER_TIER_INDEX = 1
    }

    override fun onFlowFinished() {
    }
}
