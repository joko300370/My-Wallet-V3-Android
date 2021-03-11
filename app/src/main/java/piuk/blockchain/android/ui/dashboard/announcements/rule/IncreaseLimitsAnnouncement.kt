package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import com.blockchain.preferences.SimpleBuyPrefs
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

class IncreaseLimitsAnnouncement(
    dismissRecorder: DismissRecorder,
    private val announcementQueries: AnnouncementQueries,
    private val simpleBuyPrefs: SimpleBuyPrefs
) : AnnouncementRule(dismissRecorder) {
    override val dismissKey = DISMISS_KEY
    override val name: String
        get() = "increase_limits"

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed)
            return Single.just(false)
        return announcementQueries.isGoldComplete().flatMap { isGold ->
            if (!isGold) {
                announcementQueries.isSimplifiedDueDiligenceVerified().map { verified ->
                    verified && simpleBuyPrefs.hasCompletedAtLeastOneBuy
                }
            } else
                Single.just(false)
        }
    }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = StandardAnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardPeriodic,
                dismissEntry = dismissEntry,
                titleText = R.string.increase_your_limits,
                bodyText = R.string.increase_your_limits_body,
                ctaText = R.string.continue_to_gold,
                iconImage = R.drawable.ic_update_to_gold,
                dismissFunction = {
                    host.dismissAnnouncementCard()
                },
                ctaFunction = {
                    host.dismissAnnouncementCard()
                    host.startKyc(CampaignType.None)
                }
            )
        )
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        private const val DISMISS_KEY = "IncreaseLimitsAnnouncement_DISMISSED"
    }
}