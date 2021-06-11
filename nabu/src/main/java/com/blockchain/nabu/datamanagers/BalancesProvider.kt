package com.blockchain.nabu.datamanagers

import com.blockchain.nabu.models.responses.simplebuy.AllAssetBalancesResponse
import io.reactivex.Single

interface BalancesProvider {
    fun getCustodialWalletBalanceForAllAssets(): Single<AllAssetBalancesResponse>
}