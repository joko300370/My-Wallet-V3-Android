package com.blockchain.swap.nabu.datamanagers.repositories

import com.blockchain.rx.TimedCacheRequest
import com.blockchain.swap.nabu.datamanagers.LimitsProvider
import com.blockchain.swap.nabu.models.interest.InterestLimits
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Maybe

class InterestLimitsRepository(limitsProvider: LimitsProvider) {

    private val cache = TimedCacheRequest(
        cacheLifetimeSeconds = CACHE_LIFETIME,
        refreshFn = { limitsProvider.getLimitsForAllAssets() }
    )

    fun getLimitForAsset(ccy: CryptoCurrency): Maybe<InterestLimits> =
        cache.getCachedSingle().flatMapMaybe { limitsList ->
            val limitsForAsset = limitsList.list.find { it.cryptoCurrency == ccy }
            limitsForAsset?.let {
                Maybe.just(it)
            } ?: Maybe.empty()
        }.onErrorResumeNext(Maybe.empty())

    companion object {
        private const val CACHE_LIFETIME = 240L
    }
}
