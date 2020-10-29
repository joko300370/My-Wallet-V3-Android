package com.blockchain.swap.nabu.datamanagers.repositories.swap

import com.blockchain.rx.TimedCacheRequest
import com.blockchain.swap.nabu.datamanagers.SwapDirection
import com.blockchain.swap.nabu.datamanagers.SwapPair
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single

class SwapRepository(pairsProvider: SwapPairsProvider, activityProvider: SwapActivityProvider) {

    private val swapPairsCache = TimedCacheRequest(
        cacheLifetimeSeconds = LONG_CACHE,
        refreshFn = {
            pairsProvider.getAvailablePairs()
        }
    )

    private val swapActivityCache = TimedCacheRequest(
        cacheLifetimeSeconds = SHORT_CACHE,
        refreshFn = {
            activityProvider.getSwapActivity()
        }
    )

    fun getSwapAvailablePairs(): Single<List<SwapPair>> =
        swapPairsCache.getCachedSingle()

    fun getSwapActivityForAsset(
        cryptoCurrency: CryptoCurrency,
        directions: List<SwapDirection>
    ): Single<List<SwapTransactionItem>> =
        swapActivityCache.getCachedSingle().map { list ->
            list.filter {
                it.sendingAsset == cryptoCurrency && directions.contains(it.direction)
            }
        }

    companion object {
        const val LONG_CACHE = 60000L
        const val SHORT_CACHE = 120L
    }
}