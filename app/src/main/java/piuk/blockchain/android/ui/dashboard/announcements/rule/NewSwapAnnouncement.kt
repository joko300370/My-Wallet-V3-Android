package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import com.blockchain.swap.nabu.datamanagers.EligibilityProvider
import com.blockchain.swap.nabu.service.TierService
import io.reactivex.Single
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder

class NewSwapAnnouncement(
    tiersService: TierService,
    eligibilityProvider: EligibilityProvider,
    dismissRecorder: DismissRecorder
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey =
        NewSwapAnnouncement.DISMISS_KEY

    override val name: String
        get() = "swapi"

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        return kycTiersQueries.isKycResubmissionRequired()
    }

    override fun show(host: AnnouncementHost) {
        TODO("Not yet implemented")
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "SWAP_NEW_ANNOUNCEMENT_DISMISSED"
    }
}