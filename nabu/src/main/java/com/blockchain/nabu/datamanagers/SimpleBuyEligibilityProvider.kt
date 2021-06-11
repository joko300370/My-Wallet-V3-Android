package com.blockchain.nabu.datamanagers

import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.service.NabuService
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.rx.ParameteredSingleTimedCacheRequest
import io.reactivex.Single

interface SimpleBuyEligibilityProvider {
    val defCurrency: String
    fun isEligibleForSimpleBuy(currency: String = defCurrency, forceRefresh: Boolean = false): Single<Boolean>
}

class NabuCachedEligibilityProvider(
    private val nabuService: NabuService,
    private val authenticator: Authenticator,
    private val currencyPrefs: CurrencyPrefs
) : SimpleBuyEligibilityProvider {
    override val defCurrency: String
        get() = currencyPrefs.selectedFiatCurrency

    private val refresh: (String) -> Single<Boolean> = { currency ->
        authenticator.authenticate {
            nabuService.isEligibleForSimpleBuy(it, currency)
        }.map {
            it.simpleBuyTradingEligible
        }.onErrorReturn {
            false
        }
    }

    private val cache = ParameteredSingleTimedCacheRequest(
        cacheLifetimeSeconds = 20L,
        refreshFn = refresh
    )

    override fun isEligibleForSimpleBuy(currency: String, forceRefresh: Boolean): Single<Boolean> {
        return if (!forceRefresh) cache.getCachedSingle(currency) else refresh(currency)
    }
}

class MockedEligibilityProvider(private val isEligible: Boolean) : SimpleBuyEligibilityProvider {
    override val defCurrency: String
        get() = ""

    override fun isEligibleForSimpleBuy(currency: String, forceRefresh: Boolean): Single<Boolean> =
        Single.just(isEligible)
}