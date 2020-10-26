package com.blockchain.swap.nabu.datamanagers.repositories

import com.blockchain.rx.TimedCacheRequest
import com.blockchain.swap.nabu.Authenticator
import com.blockchain.swap.nabu.datamanagers.SwapPair
import com.blockchain.swap.nabu.service.NabuService
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single

class SwapPairsRepository(pairsProvider: SwapPairsProvider) {

    private val cache = TimedCacheRequest(
        cacheLifetimeSeconds = CACHE_LIFETIME,
        refreshFn = {
            pairsProvider.getAvailablePairs()
        }
    )

    fun getSwapAvailablePairs(): Single<List<SwapPair>> =
        cache.getCachedSingle()

    companion object {
        const val CACHE_LIFETIME = 60000L
    }
}

interface SwapPairsProvider {
    fun getAvailablePairs(): Single<List<SwapPair>>
}

class SwapPairsProviderImpl(
    private val authenticator: Authenticator,
    private val nabuService: NabuService
) : SwapPairsProvider {
    override fun getAvailablePairs(): Single<List<SwapPair>> = authenticator.authenticate { sessionToken ->
        nabuService.getSwapAvailablePairs(sessionToken)
    }.map { response ->
        response.mapNotNull { pair ->
            val parts = pair.split("-")
            val source = CryptoCurrency.fromNetworkTicker(parts[0]) ?: return@mapNotNull null
            val destination =
                parts.takeIf { it.size == 2 }
                    ?.let { CryptoCurrency.fromNetworkTicker(parts[1]) ?: return@mapNotNull null }
                    ?: return@mapNotNull null
            SwapPair(
                source = source,
                destination = destination
            )
        }
    }
}