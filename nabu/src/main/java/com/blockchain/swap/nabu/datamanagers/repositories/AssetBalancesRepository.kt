package com.blockchain.swap.nabu.datamanagers.repositories

import com.blockchain.rx.TimedCacheRequest
import com.blockchain.swap.nabu.datamanagers.BalancesProvider
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Maybe

class AssetBalancesRepository(balancesProvider: BalancesProvider) {

    val cache = TimedCacheRequest(
        cacheLifetimeSeconds = CACHE_LIFETIME,
        refreshFn = { balancesProvider.getBalanceForAllAssets() }
    )

    fun getBalanceForAsset(ccy: CryptoCurrency): Maybe<CryptoValue> =
        cache.getCachedSingle().flatMapMaybe {
            it[ccy]?.let {
                Maybe.just(CryptoValue.fromMinor(ccy, it.toBigInteger()))
            } ?: Maybe.empty()
        }

    fun getBalanceForAsset(fiat: String): Maybe<FiatValue> =
        cache.getCachedSingle().flatMapMaybe {
            // TODO remove dummy data
            if (fiat == "EUR") {
                Maybe.just(FiatValue.fromMinor(fiat, "2000".toLong()))
            } /*else if (fiat == "GBP") {
                Maybe.just(FiatValue.fromMinor(fiat, "50000".toLong()))
            } */else {
                it[fiat]?.let {
                    Maybe.just(FiatValue.fromMinor(fiat, it.toLong()))
                } ?: Maybe.empty()
            }
        }

    companion object {
        private const val CACHE_LIFETIME = 10L
    }
}
