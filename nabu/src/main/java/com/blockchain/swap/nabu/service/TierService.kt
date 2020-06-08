package com.blockchain.swap.nabu.service

import com.blockchain.swap.nabu.models.nabu.KycTiers
import io.reactivex.Single

interface TierService {

    fun tiers(): Single<KycTiers>
}
