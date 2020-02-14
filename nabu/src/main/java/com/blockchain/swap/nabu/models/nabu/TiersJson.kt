package com.blockchain.swap.nabu.models.nabu

import com.blockchain.serialization.JsonSerializable
import info.blockchain.balance.FiatValue
import java.math.BigDecimal

data class TiersJson(
    val tiers: List<TierJson>
) : JsonSerializable {

    val combinedState: Kyc2TierState
        get() {
            val tier2State = tiers[2].state
            return if (tier2State == KycTierState.None) {
                when (tiers[1].state) {
                    KycTierState.None -> Kyc2TierState.Locked
                    KycTierState.Pending -> Kyc2TierState.Tier1Pending
                    KycTierState.Rejected -> Kyc2TierState.Tier1Failed
                    KycTierState.Verified -> Kyc2TierState.Tier1Approved
                    KycTierState.Under_Review -> Kyc2TierState.Tier1InReview
                }
            } else {
                when (tier2State) {
                    KycTierState.None -> Kyc2TierState.Locked
                    KycTierState.Pending -> Kyc2TierState.Tier2InPending
                    KycTierState.Rejected -> Kyc2TierState.Tier2Failed
                    KycTierState.Verified -> Kyc2TierState.Tier2Approved
                    KycTierState.Under_Review -> Kyc2TierState.Tier2InReview
                }
            }
        }
}

data class TierJson(
    val index: Int,
    val name: String,
    val state: KycTierState,
    val limits: LimitsJson
) : JsonSerializable

data class LimitsJson(
    val currency: String,
    val daily: BigDecimal?,
    val annual: BigDecimal?
) : JsonSerializable {

    val dailyFiat: FiatValue? get() = daily?.let { FiatValue.fromMajor(currency, it) }

    val annualFiat: FiatValue? get() = annual?.let { FiatValue.fromMajor(currency, it) }
}

enum class KycTierState {
    None,
    Rejected,
    Pending,
    Verified,
    Under_Review,
}

enum class Kyc2TierState {
    Hidden,
    Locked,
    Tier1Pending,
    Tier1Approved,
    Tier1Failed,
    Tier1InReview,
    Tier2InPending,
    Tier2Approved,
    Tier2Failed,
    Tier2InReview;
}

val goldTierComplete = listOf(
    Kyc2TierState.Tier2InPending,
    Kyc2TierState.Tier2Approved,
    Kyc2TierState.Tier2Failed
)

val kycVerified = listOf(
    Kyc2TierState.Tier1Approved,
    Kyc2TierState.Tier2Approved
)