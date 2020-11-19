package com.blockchain.swap.nabu.datamanagers.repositories.swap

import com.blockchain.rx.TimedCacheRequest
import com.blockchain.swap.nabu.datamanagers.CurrencyPair
import com.blockchain.swap.nabu.datamanagers.TransferDirection
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single

class SwapRepository(pairsProvider: TradingPairsProvider, activityProvider: SwapActivityProvider) {

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

    fun getSwapAvailablePairs(): Single<List<CurrencyPair.CryptoCurrencyPair>> =
        swapPairsCache.getCachedSingle().map { it.filterIsInstance<CurrencyPair.CryptoCurrencyPair>() }

    fun getSellAvailablePairs(): Single<List<CurrencyPair.CryptoToFiatCurrencyPair>> =
        swapPairsCache.getCachedSingle().map {
            it.filterIsInstance<CurrencyPair.CryptoToFiatCurrencyPair>()
        }

    fun getSwapActivityForAsset(
        cryptoCurrency: CryptoCurrency,
        directions: List<TransferDirection>
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