package com.blockchain.swap.nabu.datamanagers.featureflags

import com.blockchain.swap.nabu.datamanagers.NabuUserRepository
import com.blockchain.swap.nabu.models.nabu.KycState
import io.reactivex.Single

class KycFeatureEligibility(private val userRepository: NabuUserRepository) : FeatureEligibility {
    override fun isEligibleFor(feature: Feature): Single<Boolean> =
        when (feature) {
            Feature.INTEREST_RATES,
            Feature.INTEREST_DETAILS,
            Feature.SIMPLEBUY_BALANCE ->
                Single.fromObservable(
                    userRepository.fetchUser().map {
                        it.currentTier == 2 && it.kycState == KycState.Verified
                    }
                )
        }
}