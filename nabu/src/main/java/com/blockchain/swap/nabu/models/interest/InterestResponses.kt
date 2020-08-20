package com.blockchain.swap.nabu.models.interest

data class InterestResponse(
    val rate: Double
)

data class InterestAccountBalanceResponse(
    val balance: String
)

data class InterestAddressResponse(
    val accountRef: String
)

data class InterestLimitsFullResponse(
    val limits: AssetLimitsResponse
)

data class AssetLimitsResponse(
    val assetMap: Map<String, InterestLimitsResponse>
)

data class InterestLimitsResponse(
    val currency: String,
    val lockUpDuration: Int,
    val maxWithdrawalAmount: Int,
    val minDepositAmount: Long
)