package piuk.blockchain.android.identity

import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import io.reactivex.Single

interface UserIdentity {
    fun isEligibleFor(feature: Feature): Single<Boolean>
    fun isVerifiedFor(feature: Feature): Single<Boolean>
}

sealed class Feature {
    class TierLevel(val tierLevel: KycTierLevel) : Feature()
    object SimplifiedDueDiligence : Feature()
}