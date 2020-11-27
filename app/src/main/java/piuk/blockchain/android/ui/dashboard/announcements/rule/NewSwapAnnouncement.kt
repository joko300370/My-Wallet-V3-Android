package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import com.blockchain.swap.nabu.datamanagers.EligibilityProvider
import com.blockchain.swap.nabu.service.TierService
import io.reactivex.Single
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder

class NewSwapAnnouncement(
    private val tiersService: TierService,
    private val eligibilityProvider: EligibilityProvider,
    dismissRecorder: DismissRecorder
) : AnnouncementRule(dismissRecorder) {

    private var suggestedAnnouncement: Int = 0

    override val dismissKey: String
        get() = DISMISS_KEY.plus(suggestedAnnouncement.toString())

    override val name: String
        get() = "swap_announcements"

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
            dismissEntry.isDismissed
        }

    override fun show(host: AnnouncementHost) {
        TODO("Not yet implemented")
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "SWAP_NEW_ANNOUNCEMENT_DISMISSED"
    }
}