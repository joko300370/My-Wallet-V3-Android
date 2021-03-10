package piuk.blockchain.android.identity

import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.service.TierService
import io.reactivex.Single

class NabuUserIdentity(
    private val custodialWalletManager: CustodialWalletManager,
    private val tierService: TierService
) : UserIdentity {
    override fun isEligibleFor(feature: Feature): Single<Boolean> {
        return when (feature) {
            is Feature.TierLevel -> tierService.tiers().map {
                it.isNotInitialisedFor(feature.tierLevel)
            }
            is Feature.SimplifiedDueDiligence -> custodialWalletManager.isSimplifiedDueDiligenceEligible()
        }
    }

    override fun isVerifiedFor(feature: Feature): Single<Boolean> {
        return when (feature) {
            is Feature.TierLevel -> tierService.tiers().map {
                it.isApprovedFor(feature.tierLevel)
            }
            is Feature.SimplifiedDueDiligence -> custodialWalletManager.fetchSimplifiedDueDiligenceUserState().map {
                it.isVerified
            }
        }
    }
}