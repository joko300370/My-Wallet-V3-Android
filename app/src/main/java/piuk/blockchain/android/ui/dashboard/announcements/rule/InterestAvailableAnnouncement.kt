package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

class InterestAvailableAnnouncement(
    dismissRecorder: DismissRecorder
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        return Single.just(true)
    }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = StandardAnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardPeriodic,
                dismissEntry = dismissEntry,
                titleText = R.string.interest_announcement_title,
                bodyText = R.string.interest_announcement_description,
                iconImage = R.drawable.ic_interest_blue_circle,
                ctaText = R.string.interest_announcement_action,
                ctaFunction = {
                    host.dismissAnnouncementCard()
                    host.startInterestDashboard()
                },
                dismissFunction = {
                    host.dismissAnnouncementCard()
                }
            )
        )
    }

    override val name = "interest_available"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "InterestAvailableAnnouncement_DISMISSED"
    }
}