package com.blockchain.swap.nabu.datamanagers.repositories.interest

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Single

interface InterestLimitsProvider {
    fun getLimitsForAllAssets(): Single<InterestLimitsList>
}

data class InterestLimits(
    val interestLockUpDuration: Int,
    val minDepositAmount: CryptoValue,
    val cryptoCurrency: CryptoCurrency,
    val currency: String
)

data class InterestLimitsList(
    val list: List<InterestLimits>
)