package com.blockchain.nabu.datamanagers.repositories

import com.blockchain.nabu.datamanagers.BalancesProvider
import com.blockchain.rx.TimedCacheRequest
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Maybe
import timber.log.Timber

class CustodialAssetWalletsBalancesRepository(balancesProvider: BalancesProvider) {

    private val custodialBalancesCache = TimedCacheRequest(
        cacheLifetimeSeconds = CACHE_LIFETIME,
        refreshFn = {
            balancesProvider.getCustodialWalletBalanceForAllAssets()
                .doOnSuccess { Timber.d("Custodial balance response: $it") }
        }
    )

    private val interestBalancesCache = TimedCacheRequest(
        cacheLifetimeSeconds = CACHE_LIFETIME,
        refreshFn = {
            balancesProvider.getInterestWalletBalanceForAllAssets()
                .doOnSuccess { Timber.d("Interest balance response: $it") }
        }
    )

    fun getInterestActionableBalance(ccy: CryptoCurrency): Maybe<CryptoValue> =
        interestBalancesCache.getCachedSingle().flatMapMaybe {
            it[ccy]?.let { response ->
                Maybe.just(CryptoValue.fromMinor(ccy, response.actionable.toBigInteger()))
            } ?: Maybe.empty()
        }.onErrorResumeNext(Maybe.empty())

    fun getCustodialTotalBalanceForAsset(ccy: CryptoCurrency): Maybe<CryptoValue> =
        custodialBalancesCache.getCachedSingle().flatMapMaybe {
            it[ccy]?.let { response ->
                Maybe.just(CryptoValue.fromMinor(ccy, response.total.toBigInteger()))
            } ?: Maybe.empty()
        }.onErrorResumeNext(Maybe.empty())

    fun getCustodialActionableBalanceForAsset(ccy: CryptoCurrency): Maybe<CryptoValue> =
        custodialBalancesCache.getCachedSingle().flatMapMaybe {
            it[ccy]?.let { response ->
                Maybe.just(CryptoValue.fromMinor(ccy, response.actionable.toBigInteger()))
            } ?: Maybe.empty()
        }.onErrorResumeNext(Maybe.empty())

    fun getCustodialPendingBalanceForAsset(ccy: CryptoCurrency): Maybe<CryptoValue> =
        custodialBalancesCache.getCachedSingle().flatMapMaybe {
            it[ccy]?.let { response ->
                Maybe.just(CryptoValue.fromMinor(ccy, response.pending.toBigInteger()))
            } ?: Maybe.empty()
        }.onErrorResumeNext(Maybe.empty())

    fun getFiatTotalBalanceForAsset(fiat: String): Maybe<FiatValue> =
        custodialBalancesCache.getCachedSingle().flatMapMaybe {
            it[fiat]?.let { response ->
                Maybe.just(FiatValue.fromMinor(fiat, response.total.toLong()))
            } ?: Maybe.empty()
        }.onErrorResumeNext(Maybe.empty())

    fun getFiatActionableBalanceForAsset(fiat: String): Maybe<FiatValue> =
        custodialBalancesCache.getCachedSingle().flatMapMaybe {
            it[fiat]?.let { response ->
                Maybe.just(FiatValue.fromMinor(fiat, response.actionable.toLong()))
            } ?: Maybe.empty()
        }.onErrorResumeNext(Maybe.empty())

    fun getFiatPendingBalanceForAsset(fiat: String): Maybe<FiatValue> =
        custodialBalancesCache.getCachedSingle().flatMapMaybe {
            it[fiat]?.let { response ->
                Maybe.just(FiatValue.fromMinor(fiat, response.pending.toLong()))
            } ?: Maybe.empty()
        }.onErrorResumeNext(Maybe.empty())

    companion object {
        private const val CACHE_LIFETIME = 10L
    }
}
