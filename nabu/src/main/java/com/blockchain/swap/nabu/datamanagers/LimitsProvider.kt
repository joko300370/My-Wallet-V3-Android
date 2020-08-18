package com.blockchain.swap.nabu.datamanagers

import com.blockchain.swap.nabu.models.interest.InterestLimitsList
import io.reactivex.Single

interface LimitsProvider {
    fun getLimitsForAllAssets(): Single<InterestLimitsList>
}