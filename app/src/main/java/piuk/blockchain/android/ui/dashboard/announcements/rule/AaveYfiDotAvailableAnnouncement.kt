package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

class AaveYfiDotAvailableAnnouncement(
    dismissRecorder: DismissRecorder
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY

    override fun shouldShow(): Single<Boolean> = Single.just(!dismissEntry.isDismissed)

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = StandardAnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardOneTime,
                dismissEntry = dismissEntry,
                titleText = R.string.aave_yfi_dot_available_card_title,
                bodyText = R.string.aave_yfi_dot_available_card_body,
                ctaText = R.string.aave_yfi_dot_available_card_cta,
                iconImage = R.drawable.vector_aave_yfi_dot_announcement,
                shouldWrapIconWidth = true,
                dismissFunction = {
                    host.dismissAnnouncementCard()
                },
                ctaFunction = {
                    host.dismissAnnouncementCard()
                    host.startBuy()
                }
            )
        )
    }

    override val name = "aave_yfi_dot_available"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "AaveYFIDotAvailableAnnouncement_DISMISSED"
    }
}