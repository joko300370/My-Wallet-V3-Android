package com.blockchain.swap.nabu.datamanagers

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.Authenticator
import com.blockchain.swap.nabu.models.interest.InterestLimits
import com.blockchain.swap.nabu.models.interest.InterestLimitsList
import com.blockchain.swap.nabu.service.NabuService
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single

class LimitsProviderImpl(
    private val nabuService: NabuService,
    private val authenticator: Authenticator,
    private val currencyPrefs: CurrencyPrefs
) : LimitsProvider {
    override fun getLimitsForAllAssets(): Single<InterestLimitsList> =
        authenticator.authenticate {
            nabuService.getInterestLimits(it, currencyPrefs.selectedFiatCurrency).flatMap {
                val assetList = InterestLimitsList()
                it.body()?.let { responseBody ->
                    assetList.list.add(
                        InterestLimits(
                            responseBody.limits.BTC.lockUpDuration,
                            responseBody.limits.BTC.minDepositAmount,
                            CryptoCurrency.BTC,
                            responseBody.limits.BTC.currency
                        ))
                    assetList.list.add(
                        InterestLimits(
                            responseBody.limits.ETH.lockUpDuration,
                            responseBody.limits.ETH.minDepositAmount,
                            CryptoCurrency.ETHER,
                            responseBody.limits.ETH.currency
                        ))
                    assetList.list.add(
                        InterestLimits(
                            responseBody.limits.USDT.lockUpDuration,
                            responseBody.limits.USDT.minDepositAmount,
                            CryptoCurrency.USDT,
                            responseBody.limits.USDT.currency
                        ))
                    assetList.list.add(
                        InterestLimits(
                            responseBody.limits.PAX.lockUpDuration,
                            responseBody.limits.PAX.minDepositAmount,
                            CryptoCurrency.PAX,
                            responseBody.limits.PAX.currency
                        ))
                }
                Single.just(assetList)
            }
        }
}