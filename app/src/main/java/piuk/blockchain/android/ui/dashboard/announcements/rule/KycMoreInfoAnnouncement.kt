package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import com.blockchain.swap.nabu.models.nabu.Kyc2TierState
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.swap.nabu.service.TierService
import piuk.blockchain.androidbuysell.api.CoinifyWalletService
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

internal class KycMoreInfoAnnouncement(
    private val tierService: TierService,
    private val coinifyWalletService: CoinifyWalletService,
    private val showPopupFeatureFlag: FeatureFlag,
    dismissRecorder: DismissRecorder
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey =
        DISMISS_KEY

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        return Single.merge(
            didNotStartGoldLevelKyc(),
            isCoinifyTrader(),
            showPopupFeatureFlag.enabled
        ).all { it }
    }

    private fun isCoinifyTrader(): Single<Boolean> =
        coinifyWalletService.getCoinifyData()
            .isEmpty
            .map { !it }

    private fun didNotStartGoldLevelKyc(): Single<Boolean> =
        tierService.tiers().map {
            it.combinedState !in listOf(
                Kyc2TierState.Tier2InPending,
                Kyc2TierState.Tier2Approved,
                Kyc2TierState.Tier2Failed
            )
        }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            StandardAnnouncementCard(
                name = name,
                titleText = R.string.kyc_more_info_title,
                bodyText = R.string.kyc_more_info_body,
                ctaText = R.string.kyc_more_info_cta,
                iconImage = R.drawable.ic_announce_kyc,
                dismissFunction = {
                    host.dismissAnnouncementCard()
                },
                ctaFunction = {
                    host.dismissAnnouncementCard()
                    host.startKyc(CampaignType.Sunriver)
                },
                dismissEntry = dismissEntry,
                dismissRule = DismissRule.CardOneTime
            )
        )
    }

    override val name = "kyc_more_info"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "CoinifyKycModalPopup_DISMISSED"
    }
}
