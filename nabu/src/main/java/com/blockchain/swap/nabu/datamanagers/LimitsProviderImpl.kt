package com.blockchain.swap.nabu.datamanagers

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.Authenticator
import com.blockchain.swap.nabu.models.interest.InterestLimits
import com.blockchain.swap.nabu.models.interest.InterestLimitsList
import com.blockchain.swap.nabu.service.NabuService
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import io.reactivex.Single

class LimitsProviderImpl(
    private val nabuService: NabuService,
    private val authenticator: Authenticator,
    private val currencyPrefs: CurrencyPrefs
) : LimitsProvider {
    override fun getLimitsForAllAssets(): Single<InterestLimitsList> =
        authenticator.authenticate {
            nabuService.getInterestLimits(it, currencyPrefs.selectedFiatCurrency)
                .map { response ->
                    val list = response.body()?.let { responseBody ->
                        responseBody.limits.assetMap.entries.map { entry ->
                            InterestLimits(
                                entry.value.lockUpDuration,
                                FiatValue.fromMinor(entry.value.currency, entry.value.minDepositAmount),
                                CryptoCurrency.fromNetworkTicker(entry.key)!!,
                                entry.value.currency
                            )
                        }
                    } ?: emptyList()

                    InterestLimitsList(list)
                }
        }
}