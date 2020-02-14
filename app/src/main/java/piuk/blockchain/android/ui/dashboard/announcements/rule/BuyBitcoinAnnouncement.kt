package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import com.blockchain.preferences.WalletStatus
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.SimpleBuyAvailability
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager

class BuyBitcoinAnnouncement(
    dismissRecorder: DismissRecorder,
    private val walletStatus: WalletStatus,
    private val buyDataManager: BuyDataManager,
    private val simpleBuyAvailability: SimpleBuyAvailability
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        return buyDataManager.canBuy.zipWith(simpleBuyAvailability.isAvailable())
            .map { (canBuy, simpleBuyAvailable) ->
                canBuy && !simpleBuyAvailable && !walletStatus.isWalletFunded
            }
    }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = StandardAnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardPeriodic,
                dismissEntry = dismissEntry,
                titleText = R.string.buy_btc_card_title,
                bodyText = R.string.buy_btc_card_body,
                ctaText = R.string.buy_btc_card_cta,
                iconImage = R.drawable.ic_announce_buy_btc,
                dismissFunction = {
                    host.dismissAnnouncementCard()
                },
                ctaFunction = {
                    host.dismissAnnouncementCard()
                    host.startBuySell()
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
