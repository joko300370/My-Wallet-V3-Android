package com.blockchain.swap.nabu.datamanagers.repositories.swap

import com.blockchain.swap.nabu.Authenticator
import com.blockchain.swap.nabu.datamanagers.CurrencyPair
import com.blockchain.swap.nabu.datamanagers.custodialwalletimpl.LiveCustodialWalletManager
import com.blockchain.swap.nabu.service.NabuService
import io.reactivex.Single

interface TradingPairsProvider {
    fun getAvailablePairs(): Single<List<CurrencyPair>>
}

class TradingPairsProviderImpl(
    private val authenticator: Authenticator,
    private val nabuService: NabuService
) : TradingPairsProvider {
    override fun getAvailablePairs(): Single<List<CurrencyPair>> = authenticator.authenticate { sessionToken ->
        nabuService.getSwapAvailablePairs(sessionToken)
    }.map { response ->
        response.mapNotNull { pair ->
            val parts = pair.split("-")
            if (parts.size != 2) return@mapNotNull null
            CurrencyPair.fromRawPair(pair, LiveCustodialWalletManager.SUPPORTED_FUNDS_CURRENCIES)
        }
    }
}