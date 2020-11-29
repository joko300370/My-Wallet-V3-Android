package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.blockchain.swap.nabu.datamanagers.EligibilityProvider
import com.blockchain.swap.nabu.service.TierService
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

class NewSwapAnnouncement(
    private val tiersService: TierService,
    private val eligibilityProvider: EligibilityProvider,
    dismissRecorder: DismissRecorder
) : AnnouncementRule(dismissRecorder) {

    private var suggestedAnnouncement: Int = 0

    override val dismissKey: String
        get() = DISMISS_KEY.plus(suggestedAnnouncement.toString())

    override val name: String
        get() = "swap_v2"

    override fun shouldShow(): Single<Boolean> =
        tiersService.tiers().map {
            it.isVerified()
        }.flatMap {
            if (!it)
                Single.just(0)
            else eligibilityProvider.isEligibleForSimpleBuy().map { eligible ->
                if (eligible) 2
                else 1
            }
        }.doOnSuccess {
            suggestedAnnouncement = it
        }.map {
            !dismissEntry.isDismissed
        }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = StandardAnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardOneTime,
                dismissEntry = dismissEntry,
                titleText = when (suggestedAnnouncement) {
                    0 -> R.string.airdrop_received_sheet_heading
                    1 -> R.string.kyc_settings_status_rejected
                    else -> R.string.kyc_status_title_approved
                },
                bodyText = R.string.wdgld_available_card_body,
                ctaText = R.string.wdgld_available_card_cta,
                iconImage = R.drawable.vector_dgld_colored,
                dismissFunction = {
                    host.dismissAnnouncementCard()
                },
                ctaFunction = {
                    host.dismissAnnouncementCard()
                }
            )
        )
    }

    companion object {
        private const val DISMISS_KEY = "SWAP_NEW_ANNOUNCEMENT_DISMISSED"
    }
}