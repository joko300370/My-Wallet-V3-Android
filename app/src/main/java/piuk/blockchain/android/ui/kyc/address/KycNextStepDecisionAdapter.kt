package piuk.blockchain.android.ui.kyc.address

import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.datamanagers.NabuDataManager
import io.reactivex.Single

internal class KycNextStepDecisionAdapter(
    private val nabuToken: NabuToken,
    private val nabuDataManager: NabuDataManager
) : KycNextStepDecision {

    override fun nextStep(): Single<KycNextStepDecision.NextStep> =
        nabuToken.fetchNabuToken()
            .flatMap(nabuDataManager::getUser)
            .map { user ->
                if (user.tierInProgressOrCurrentTier == 1) {
                    KycNextStepDecision.NextStep.Tier1Complete
                } else {
                    val tiers = user.tiers
                    if (tiers == null || tiers.next ?: 0 > tiers.selected ?: 0) {
                        // the backend is telling us the user should be put down path for tier2 even though they
                        // selected tier 1, so we need to inform them
                        KycNextStepDecision.NextStep.Tier2ContinueTier1NeedsMoreInfo
                    } else {
                        KycNextStepDecision.NextStep.Tier2Continue
                    }
                }
            }
}
