package com.blockchain.nabu.datamanagers

import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.models.responses.simplebuy.AllAssetBalancesResponse
import com.blockchain.nabu.service.NabuService
import io.reactivex.Single

class BalanceProviderImpl(
    private val nabuService: NabuService,
    private val authenticator: Authenticator
) : BalancesProvider {
    override fun getCustodialWalletBalanceForAllAssets(): Single<AllAssetBalancesResponse> =
        authenticator.authenticate {
            nabuService.getCustodialWalletBalanceForAllAssets(it)
        }
}