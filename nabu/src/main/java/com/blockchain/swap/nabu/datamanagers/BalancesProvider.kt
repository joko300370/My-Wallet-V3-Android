package com.blockchain.swap.nabu.datamanagers

import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyAllBalancesResponse
import io.reactivex.Single

interface BalancesProvider {
    fun getBalanceForAllAssets(): Single<SimpleBuyAllBalancesResponse>
}