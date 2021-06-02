package com.blockchain.nabu.datamanagers.repositories.interest

import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.datamanagers.InterestAccountDetails
import com.blockchain.nabu.models.responses.interest.InterestAccountDetailsResponse
import com.blockchain.nabu.service.NabuService
import com.blockchain.rx.ParameteredMappedSinglesTimedRequests
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Single

interface InterestBalancesProvider {
    fun getBalanceForAsset(asset: CryptoCurrency): Single<InterestAccountDetails>
    fun clearBalanceForAsset(asset: CryptoCurrency)
    fun clearBalanceForAsset(ticker: String)
}

class InterestBalancesProviderImpl(
    private val authenticator: Authenticator,
    private val nabuService: NabuService
) : InterestBalancesProvider {

    override fun getBalanceForAsset(asset: CryptoCurrency) = Single.just(cache.getCachedSingle(asset).blockingGet())

    override fun clearBalanceForAsset(asset: CryptoCurrency) {
        cache.invalidate(asset)
    }

    override fun clearBalanceForAsset(ticker: String) {
        val crypto = CryptoCurrency.fromNetworkTicker(ticker)
        crypto?.let {
            cache.invalidate(it)
        }
    }

    private val refresh: (CryptoCurrency) -> Single<InterestAccountDetails> = { currency ->
        authenticator.authenticate {
            nabuService.getInterestAccountBalance(it, currency.networkTicker).map { details ->
                details.toInterestAccountDetails(currency)
            }.toSingle(
                InterestAccountDetails(
                    balance = CryptoValue.zero(currency),
                    pendingInterest = CryptoValue.zero(currency),
                    pendingDeposit = CryptoValue.zero(currency),
                    totalInterest = CryptoValue.zero(currency),
                    lockedBalance = CryptoValue.zero(currency)
                )
            )
        }
    }

    private val cache = ParameteredMappedSinglesTimedRequests(
        cacheLifetimeSeconds = 240L,
        refreshFn = refresh
    )

    private fun InterestAccountDetailsResponse.toInterestAccountDetails(cryptoCurrency: CryptoCurrency) =
        InterestAccountDetails(
            balance = CryptoValue.fromMinor(cryptoCurrency, balance.toBigInteger()),
            pendingInterest = CryptoValue.fromMinor(cryptoCurrency, pendingInterest.toBigInteger()),
            pendingDeposit = CryptoValue.fromMinor(cryptoCurrency, pendingDeposit.toBigInteger()),
            totalInterest = CryptoValue.fromMinor(cryptoCurrency, totalInterest.toBigInteger()),
            lockedBalance = CryptoValue.fromMinor(cryptoCurrency, locked.toBigInteger())
        )
}
