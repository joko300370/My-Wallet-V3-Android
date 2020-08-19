package com.blockchain.swap.nabu.models.interest

import info.blockchain.balance.CryptoCurrency

data class InterestResponse(
    val rate: Double
)

data class InterestAccountBalanceResponse(
    val balance: String
)

data class InterestAddressResponse(
    val accountRef: String
)

data class InterestLimits(
    val interestLockUpDuration: Int,
    val minDepositAmount: Int,
    val cryptoCurrency: CryptoCurrency,
    val currency: String
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
    val minDepositAmount: Int
)

data class InterestLimitsList(
    val list: List<InterestLimits>
)