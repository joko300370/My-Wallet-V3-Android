package piuk.blockchain.android.ui.kyc.tiersplash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.navigation.NavDirections
import com.blockchain.activities.StartSwap
import piuk.blockchain.android.util.setImageDrawable
import com.blockchain.swap.nabu.models.nabu.KycTierState
import com.blockchain.swap.nabu.models.nabu.TierJson
import com.blockchain.swap.nabu.models.nabu.TiersJson
import com.blockchain.notifications.analytics.Analytics
import piuk.blockchain.android.ui.kyc.hyperlinks.renderSingleLink
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.ui.kyc.navhost.models.KycStep
import piuk.blockchain.android.ui.kyc.navigate
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.KYCAnalyticsEvents
import com.blockchain.notifications.analytics.kycTierStart
import com.blockchain.notifications.analytics.logEvent
import com.blockchain.ui.extensions.throttledClicks
import com.blockchain.ui.urllinks.URL_CONTACT_SUPPORT
import com.blockchain.ui.urllinks.URL_LEARN_MORE_REJECTED
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_kyc_tier_splash.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.androidcoreui.utils.extensions.visible
import timber.log.Timber

class KycTierSplashFragment : BaseFragment<KycTierSplashView, KycTierSplashPresenter>(),
    KycTierSplashView {

    private val presenter: KycTierSplashPresenter by inject()
    private val startSwap: StartSwap by inject()
    private val analytics: Analytics by inject()
    private val progressListener: KycProgressListener by ParentActivityDelegate(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_kyc_tier_splash)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logEvent(AnalyticsEvents.KycTiers)

        val showContent = arguments?.getBoolean(KycNavHostActivity.EXTRA_SHOW_TIERS_LIMITS_SPLASH) ?: false

        val title = when (progressListener.campaignType) {
            CampaignType.BuySell -> R.string.buy_sell_splash_title
            CampaignType.Swap -> R.string.kyc_splash_title
            CampaignType.Sunriver,
            CampaignType.SimpleBuy,
            CampaignType.Blockstack,
            CampaignType.Resubmission -> R.string.sunriver_splash_title
        }
        container.visibility = if (showContent) View.VISIBLE else View.GONE
        progressListener.setHostTitle(title)
        progressListener.incrementProgress(KycStep.SplashPage)

        textViewEligible.renderSingleLink(
            R.string.by_completing_gold_level_you_will_be_eligible_to_participate_in_our_airdrop_program,
            R.string.learn_more,
            R.string.airdrop_learn_more_url
        )

        onViewReady()
    }

    private val disposable = CompositeDisposable()

    override fun renderTiersList(tiers: TiersJson) {
        // Logic is now limited to 2 tiers, future refactor to traverse tiersList
        renderTier1(tiers.tiers[1])

        renderTier2(tiers.tiers[2])

        reportState(tiers.tiers[1].state, tiers.tiers[2].state)
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

    private fun renderTier(tier: TierJson, layoutElements: TierLayoutElements) {
        when (tier.state) {
            KycTierState.Rejected -> {
                layoutElements.icon.setImageDrawable(R.drawable.vector_tier_locked)
                text_header_tiers_line1.text = getString(R.string.swap_unavailable)
                text_header_tiers_line2.text = getString(R.string.swap_unavailable_explained)
                layoutElements.cardTier.alpha = 0.2F
                text_contact_support.visible()
                button_learn_more.visible()
                button_swap_now.gone()
            }
            KycTierState.Pending -> {
                layoutElements.icon.setImageDrawable(R.drawable.vector_tier_review)
                layoutElements.textTierState.visible()
                layoutElements.textTierState.text = getString(R.string.in_review)
                layoutElements.textTierState.setTextColor(
                    ContextCompat.getColor(
                        context!!,
                        R.color.kyc_in_progress
                    )
                )
                text_header_tiers_line2.text = getString(R.string.tier_x_in_review, getLevelForTier(tier))
                button_learn_more.gone()
                text_contact_support.gone()
            }
            KycTierState.Verified -> {
                layoutElements.icon.setImageDrawable(R.drawable.vector_tier_verified)
                layoutElements.textTierState.visible()
                layoutElements.textTierState.text = getString(R.string.approved)
                tier_available_fiat.text = getLimitForTier(tier)
                tier_available_fiat.visible()
                text_header_tiers_line1.text = getString(R.string.available)
                text_header_tiers_line2.text = getString(R.string.swap_limit)
                button_swap_now.visible()
            }
            else -> {
                layoutElements.textTierRequires.visible()
                layoutElements.icon.setImageDrawable(R.drawable.vector_tier_start)
                button_learn_more.gone()
                text_contact_support.gone()
            }
        }
        layoutElements.textLimit.text = getLimitForTier(tier)
        layoutElements.textPeriodicLimit.text = getString(getLimitString(tier))
    }

    private fun renderTier1(tier: TierJson) {
        val layoutElements = TierLayoutElements(
            cardTier = card_tier_1,
            icon = icon_tier1_state,
            textLevel = text_tier1_level,
            textLimit = text_tier1_limit,
            textPeriodicLimit = text_tier1_periodic_limit,
            textTierState = text_tier1_state,
            textTierRequires = text_tier1_requires
        )

        renderTier(tier, layoutElements)
    }

    private fun renderTier2(tier: TierJson) {
        val layoutElements = TierLayoutElements(
            cardTier = card_tier_2,
            icon = icon_tier2_state,
            textLevel = text_tier2_level,
            textLimit = text_tier2_limit,
            textPeriodicLimit = text_tier2_periodic_limit,
            textTierState = text_tier2_state,
            textTierRequires = text_tier2_requires
        )

        renderTier(tier, layoutElements)
    }

    private fun getLevelForTier(tier: TierJson): String =
        when (tier.index) {
            1 -> getString(R.string.silver_level)
            2 -> getString(R.string.gold_level)
            else -> tier.name
        }

    private fun getLimitForTier(tier: TierJson): String? {
        val limits = tier.limits
        return (limits.annualFiat ?: limits.dailyFiat)?.toStringWithSymbol()
    }

    @StringRes
    private fun getLimitString(tier: TierJson): Int {
        val limits = tier.limits
        return when {
            limits.annual != null -> R.string.annual_swap_limit
            limits.daily != null -> R.string.daily_swap_limit
            else -> 0
        }
    }

    override fun onResume() {
        super.onResume()
        disposable += view!!.findViewById<View>(R.id.card_tier_1)
            .throttledClicks()
            .subscribeBy(
                onNext = {
                    analytics.logEvent(KYCAnalyticsEvents.Tier1Clicked)
                    presenter.tier1Selected()
                },
                onError = { Timber.e(it) }
            )
        disposable += view!!.findViewById<View>(R.id.card_tier_2)
            .throttledClicks()
            .subscribeBy(
                onNext = {
                    analytics.logEvent(KYCAnalyticsEvents.Tier2Clicked)
                    presenter.tier2Selected()
                },
                onError = { Timber.e(it) }
            )
        disposable +=
            button_swap_now
                .throttledClicks()
                .subscribeBy(
                    onNext = {
                        startSwap.startSwapActivity(activity!!)
                        activity!!.finish()
                    },
                    onError = { Timber.e(it) }
                )
        disposable +=
            button_learn_more
                .throttledClicks()
                .subscribeBy(
                    onNext = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_LEARN_MORE_REJECTED))) },
                    onError = { Timber.e(it) }
                )
        disposable +=
            text_contact_support
                .throttledClicks()
                .subscribeBy(
                    onNext = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_CONTACT_SUPPORT))) },
                    onError = { Timber.e(it) }
                )
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

    override fun showErrorToast(message: Int) {
        toast(message, ToastCustom.TYPE_ERROR)
    }

    private inner class TierLayoutElements(
        val cardTier: CardView,
        val icon: ImageView,
        val textLevel: TextView,
        val textLimit: TextView,
        val textPeriodicLimit: TextView,
        val textTierState: TextView,
        val textTierRequires: TextView
    )
}
