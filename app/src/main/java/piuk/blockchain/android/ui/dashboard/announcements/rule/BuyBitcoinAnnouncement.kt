package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

class BuyBitcoinAnnouncement(
    dismissRecorder: DismissRecorder,
    private val announcementQueries: AnnouncementQueries
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY
    private var isUserSddEligibleButNotVerified = false

    override fun shouldShow(): Single<Boolean> {
        return announcementQueries.isSddEligibleAndNotVerified()
            .onErrorReturn { false }
            .doOnSuccess {
                isUserSddEligibleButNotVerified = it
            }
            .flatMap { Single.just(dismissEntry.isDismissed).map { !it } }
    }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = StandardAnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardPeriodic,
                dismissEntry = dismissEntry,
                titleText = R.string.buy_crypto_card_title,
                bodyText = if (isUserSddEligibleButNotVerified)
                    R.string.buy_crypto_card_sdd_body else R.string.buy_crypto_card_body,
                ctaText = R.string.buy_crypto_card_cta,
                iconImage = R.drawable.ic_announce_buy_btc,
                dismissFunction = {
                    host.dismissAnnouncementCard()
                },
                ctaFunction = {
                    host.dismissAnnouncementCard()
                    host.startSimpleBuy()
                }
            )
        )
    }

    override val name = "buy_btc"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "BuyBitcoinAuthAnnouncement_DISMISSED"
    }
}
