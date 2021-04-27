package com.blockchain.nabu.datamanagers.repositories.interest

import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single

interface InterestEnabledProvider {
    fun getEnabledStatusForAllAssets(): Single<List<CryptoCurrency>>
}
