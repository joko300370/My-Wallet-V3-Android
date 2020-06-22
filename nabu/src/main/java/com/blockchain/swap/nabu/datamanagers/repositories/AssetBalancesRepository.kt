package com.blockchain.swap.nabu.datamanagers.repositories

import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyAllBalancesResponse
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.androidcore.utils.extensions.thenMaybe
import java.util.concurrent.Semaphore

typealias RefreshFn = () -> Single<SimpleBuyAllBalancesResponse>

class AssetBalancesRepository {
    private val balanceCache = mutableMapOf<CryptoCurrency, CryptoValue>()
    private var lastUpdatedTimestamp = 0L
    private val lock = Semaphore(1)

    var fnRefresh: RefreshFn = { Single.error(IllegalStateException("Refresh fn not configured")) }

    fun getBalanceForAsset(ccy: CryptoCurrency): Maybe<CryptoValue> =
        checkRefreshCache()
            .thenMaybe {
                getBalanceFromCache(ccy)
            }

    @Synchronized
    private fun getBalanceFromCache(ccy: CryptoCurrency): Maybe<CryptoValue> =
        balanceCache[ccy]?.let {
            Maybe.just(it)
        } ?: Maybe.empty()

    private fun checkRefreshCache(): Completable {
        lock.acquire()

        return if (isCacheExpired()) {
            fnRefresh()
                .doOnSuccess {
                    updateCache(it)
                }.ignoreElement()
        } else {
            Completable.complete()
        }
        .doOnTerminate {
            lock.release()
        }
    }

    @Synchronized
    private fun updateCache(response: SimpleBuyAllBalancesResponse) {
        CryptoCurrency.activeCurrencies().forEach { ccy ->
            response[ccy]?.let {
                balanceCache[ccy] = it.toCryptoValue(ccy)
            }
        }
        lastUpdatedTimestamp = System.currentTimeMillis()
    }

    private fun isCacheExpired() =
        System.currentTimeMillis() - lastUpdatedTimestamp >= CACHE_LIFETIME

    private fun String.toCryptoValue(ccy: CryptoCurrency) =
        CryptoValue.fromMinor(ccy, this.toBigInteger())

    companion object {
        private const val CACHE_LIFETIME = 10 * 1000
    }
}
