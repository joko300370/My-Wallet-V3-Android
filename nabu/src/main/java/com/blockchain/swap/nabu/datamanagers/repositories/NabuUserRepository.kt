package com.blockchain.swap.nabu.datamanagers.repositories

import com.blockchain.rx.TimedCacheRequest
import com.blockchain.swap.nabu.datamanagers.NabuDataUserProvider
import com.blockchain.swap.nabu.models.responses.nabu.NabuUser
import io.reactivex.Single

class NabuUserRepository(nabuDataUserProvider: NabuDataUserProvider) {

    val cache = TimedCacheRequest(
        cacheLifetimeSeconds = CACHE_LIFETIME,
        refreshFn = { nabuDataUserProvider.getUser() }
    )

    fun fetchUser(): Single<NabuUser> =
        cache.getCachedSingle()

    companion object {
        private const val CACHE_LIFETIME: Long = 10
    }
}
