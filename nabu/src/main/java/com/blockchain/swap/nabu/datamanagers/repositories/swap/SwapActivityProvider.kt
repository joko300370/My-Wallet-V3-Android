package com.blockchain.swap.nabu.datamanagers.repositories.swap

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.Authenticator
import com.blockchain.swap.nabu.datamanagers.TransferDirection
import com.blockchain.swap.nabu.datamanagers.SwapOrderState
import com.blockchain.swap.nabu.datamanagers.custodialwalletimpl.toSwapState
import com.blockchain.swap.nabu.extensions.fromIso8601ToUtc
import com.blockchain.swap.nabu.service.NabuService
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.Single
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

interface SwapActivityProvider {
    fun getSwapActivity(): Single<List<SwapTransactionItem>>
}

class SwapActivityProviderImpl(
    private val authenticator: Authenticator,
    private val nabuService: NabuService,
    private val currencyPrefs: CurrencyPrefs,
    private val exchangeRates: ExchangeRateDataManager
) : SwapActivityProvider {
    override fun getSwapActivity(): Single<List<SwapTransactionItem>> =
        authenticator.authenticate { sessionToken ->
            nabuService.fetchSwapActivity(sessionToken)
        }.map { response ->
            response.mapNotNull {
                val pairSplit = it.pair.split("-")
                val sendingAsset = CryptoCurrency.fromNetworkTicker(pairSplit[0]) ?: return@mapNotNull null
                val receivingAsset = CryptoCurrency.fromNetworkTicker(pairSplit[1]) ?: return@mapNotNull null

                val apiFiat = FiatValue.fromMinor(it.fiatCurrency, it.fiatValue.toLong())
                val localFiat = apiFiat.toFiat(exchangeRates, currencyPrefs.selectedFiatCurrency)
                SwapTransactionItem(
                    it.kind.depositTxHash ?: it.id,
                    it.createdAt.fromIso8601ToUtc()!!.time,
                    it.kind.direction.mapToDirection(),
                    it.kind.depositAddress,
                    it.kind.withdrawalAddress,
                    it.state.toSwapState(),
                    CryptoValue.fromMinor(sendingAsset, it.priceFunnel.inputMoney.toBigInteger()),
                    CryptoValue.fromMinor(receivingAsset, it.priceFunnel.outputMoney.toBigInteger()),
                    getFeeForAsset(receivingAsset, it.priceFunnel.networkFee),
                    sendingAsset,
                    receivingAsset,
                    localFiat,
                    currencyPrefs.selectedFiatCurrency
                )
            }.filter {
                it.state.displayableState
            }
        }

    private fun getFeeForAsset(receivingAsset: CryptoCurrency, networkFee: String): CryptoValue =
        CryptoValue.fromMinor(
            if (receivingAsset.hasFeature(CryptoCurrency.IS_ERC20)) {
                CryptoCurrency.ETHER
            } else {
                receivingAsset
            }, networkFee.toBigInteger()
        )

    private fun String.mapToDirection(): TransferDirection =
        when (this) {
            "ON_CHAIN" -> TransferDirection.ON_CHAIN // from non-custodial to non-custodial
            "FROM_USERKEY" -> TransferDirection.FROM_USERKEY // from non-custodial to custodial
            "TO_USERKEY" -> TransferDirection.TO_USERKEY // from custodial to non-custodial - not in use currently
            "INTERNAL" -> TransferDirection.INTERNAL // from custodial to custodial
            else -> throw IllegalStateException("Unknown direction to map $this")
        }
}

data class SwapTransactionItem(
    val txId: String,
    val timeStampMs: Long,
    val direction: TransferDirection,
    val sendingAddress: String?,
    val receivingAddress: String?,
    val state: SwapOrderState,
    val sendingValue: Money,
    val receivingValue: Money,
    val withdrawalNetworkFee: CryptoValue,
    val sendingAsset: CryptoCurrency,
    val receivingAsset: CryptoCurrency,
    val fiatValue: FiatValue,
    val fiatCurrency: String
)
