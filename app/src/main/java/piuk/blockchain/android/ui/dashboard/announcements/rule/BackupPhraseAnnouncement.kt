package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import com.blockchain.preferences.WalletStatus
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

class BackupPhraseAnnouncement(
    dismissRecorder: DismissRecorder,
    private val walletStatus: WalletStatus
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        return Single.just(walletStatus.isWalletFunded && !walletStatus.isWalletBackedUp)
    }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = StandardAnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardPeriodic,
                dismissEntry = dismissEntry,
                titleText = R.string.recovery_card_title_1,
                bodyText = R.string.recovery_card_body_1,
                ctaText = R.string.recovery_card_cta,
                iconImage = R.drawable.ic_announce_backup,
                ctaFunction = {
                    host.dismissAnnouncementCard()
                    host.startFundsBackup()
                },
                dismissFunction = {
                    host.dismissAnnouncementCard()
                }
            )
        )
    }

    override val name = "backup_funds"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "BackupWalletAuthAnnouncement_DISMISSED"
    }
}
