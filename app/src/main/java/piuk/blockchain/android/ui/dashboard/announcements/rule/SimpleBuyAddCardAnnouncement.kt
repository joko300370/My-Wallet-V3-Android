package piuk.blockchain.android.ui.dashboard.announcements.rule

import android.annotation.SuppressLint
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvent
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

class SimpleBuyAddCardAnnouncement(
    dismissRecorder: DismissRecorder,
    private val analytics: Analytics,
    private val queries: AnnouncementQueries
) : AnnouncementRule(dismissRecorder) {
    override val dismissKey: String
        get() = DISMISS_KEY

    override fun shouldShow(): Single<Boolean> = queries.isKycGoldVerifiedAndHasPendingCardToAdd()

    @SuppressLint("CheckResult")
    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(createAnnouncementCard(host))
        analytics.logEvent(
            SBAddCardCardShowingAnalyticsEvent(
                SBAddCardCardShowingAnalyticsEvent.ANALYTICS_OPTION_SHOWING_CARD
            )
        )
    }

    private fun createAnnouncementCard(host: AnnouncementHost) =
        StandardAnnouncementCard(
            name = name,
            dismissEntry = dismissEntry,
            dismissRule = DismissRule.CardPersistent,
            titleText = R.string.simple_buy_add_card_card_title,
            bodyText = R.string.simple_buy_add_card_card_body,
            ctaText = R.string.simple_buy_add_card_card_cta,
            iconImage = R.drawable.ic_announce_sb_finish_signup,
            dismissFunction = {
                host.dismissAnnouncementCard()
                analytics.logEvent(
                    SBAddCardCardSeenAnalyticsEvent(
                        SBAddCardCardSeenAnalyticsEvent.ANALYTICS_DISMISS_CLOSED
                    )
                )
            },
            ctaFunction = {
                host.dismissAnnouncementCard()
                host.startSimpleBuy()
                analytics.logEvent(
                    SBAddCardCardSeenAnalyticsEvent(
                        SBAddCardCardSeenAnalyticsEvent.ANALYTICS_DISMISS_CTA_CLICK
                    )
                )
            }
        )

    override val name = "sb_pending_add_card"

    companion object {
        const val DISMISS_KEY = "SimpleBuyAddCardCard_DISMISSED"
    }

    // Sent when either the card or the popup is selected and shown.
    private class SBAddCardCardShowingAnalyticsEvent(val optionShowing: String) : AnalyticsEvent {
        override val event: String
            get() = ANALYTICS_EVENT_NAME

        override val params: Map<String, String>
            get() = mapOf(ANALYTICS_OPTION_SHOWING_PARAM to optionShowing)

        companion object {
            private const val ANALYTICS_EVENT_NAME = "sb_add_card_card_showing"
            private const val ANALYTICS_OPTION_SHOWING_PARAM = "Showing"
            const val ANALYTICS_OPTION_SHOWING_CARD = "CARD"
        }
    }

    // Fired when the card/popup is dismissed to track actions
    private class SBAddCardCardSeenAnalyticsEvent(val dismissBy: String) : AnalyticsEvent {
        override val event: String
            get() = ANALYTICS_EVENT_NAME

        override val params: Map<String, String>
            get() = mapOf(ANALYTICS_DISMISS_PARAM to dismissBy)

        companion object {
            private const val ANALYTICS_EVENT_NAME = "sb_add_card_card_seen"
            private const val ANALYTICS_DISMISS_PARAM = "Dismissed_by"
            const val ANALYTICS_DISMISS_CTA_CLICK = "CTA_CLICK"
            const val ANALYTICS_DISMISS_CLOSED = "CANCEL_CLOSE"
        }
    }
}