package com.blockchain.swap.nabu.datamanagers

import com.blockchain.swap.nabu.Authenticator
import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyAllBalancesResponse
import com.blockchain.swap.nabu.service.NabuService
import io.reactivex.Single

class BalanceProviderImpl(
    private val nabuService: NabuService,
    private val authenticator: Authenticator
) : BalancesProvider {
    override fun getBalanceForAllAssets(): Single<SimpleBuyAllBalancesResponse> =
        authenticator.authenticate {
            nabuService.getBalanceForAllAssets(it)
        }
}