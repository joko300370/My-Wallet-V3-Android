package piuk.blockchain.android.ui.dashboard.announcements.rule

import android.support.annotation.VisibleForTesting
import com.blockchain.kyc.status.KycTiersQueries
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule

internal class KycResubmissionAnnouncement(
    private val kycTiersQueries: KycTiersQueries,
    dismissRecorder: DismissRecorder
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey =
        DISMISS_KEY

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        return kycTiersQueries.isKycResubmissionRequired()
    }

    override fun show(host: AnnouncementHost) {

        val card = AnnouncementCard(
            name = name,
            titleText = R.string.kyc_resubmission_card_title,
            bodyText = R.string.kyc_resubmission_card_description,
            ctaText = R.string.kyc_resubmission_card_button,
            iconImage = R.drawable.ic_announce_kyc,
            dismissFunction = {
                host.dismissAnnouncementCard(dismissEntry.prefsKey)
            },
            ctaFunction = {
                host.dismissAnnouncementCard(dismissEntry.prefsKey)
                host.startKyc(CampaignType.Resubmission)
            },
            dismissEntry = dismissEntry,
            dismissRule = DismissRule.CardOneTime
        )
        host.showAnnouncementCard(card)
    }

    override val name = "kyc_resubmit"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "KYC_RESUBMISSION_DISMISSED"
    }
}
