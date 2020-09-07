package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

class AlgorandAvailableAnnouncement(
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
                dismissRule = DismissRule.CardOneTime,
                dismissEntry = dismissEntry,
                titleText = R.string.algorand_announcement_title,
                bodyText = R.string.algorand_announcement_description,
                iconImage = R.drawable.vector_algo_colored,
                ctaText = R.string.algorand_announcement_action,
                ctaFunction = {
                    host.dismissAnnouncementCard()
                    host.startSimpleBuy(CryptoCurrency.ALGO)
                },
                dismissFunction = {
                    host.dismissAnnouncementCard()
                }
            )
        )
    }

    override val name = "algorand_available"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "AlgorandAvailableAnnouncement_DISMISSED"
    }
}