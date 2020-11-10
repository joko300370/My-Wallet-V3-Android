package com.blockchain.swap.nabu.datamanagers.repositories

import com.blockchain.swap.nabu.Authenticator
import com.blockchain.swap.nabu.datamanagers.TransferDirection
import com.blockchain.swap.nabu.datamanagers.CurrencyPair
import com.blockchain.swap.nabu.datamanagers.PriceTier
import com.blockchain.swap.nabu.datamanagers.TransferQuote
import com.blockchain.swap.nabu.extensions.fromIso8601ToUtc
import com.blockchain.swap.nabu.extensions.toLocalTime
import com.blockchain.swap.nabu.models.swap.QuoteRequest
import com.blockchain.swap.nabu.service.NabuService
import java.util.Date

class QuotesProvider(
    private val nabuService: NabuService,
    private val authenticator: Authenticator
) {

    fun fetchQuote(product: String = "BROKERAGE", direction: TransferDirection, pair: CurrencyPair) =
        authenticator.authenticate { sessionToken ->
            nabuService.fetchQuote(sessionToken,
                QuoteRequest(
                    product = product,
                    direction = direction.toString(),
                    pair = pair.rawValue
                )).map {
                TransferQuote(
                    id = it.id,
                    prices = it.quote.priceTiers.map { price ->
                        PriceTier(
                            volume = pair.toSourceMoney(price.volume.toBigInteger()),
                            price = pair.toDestinationMoney(price.price.toBigInteger())
                        )
                    },
                    networkFee = pair.toDestinationMoney(it.networkFee.toBigInteger()),
                    staticFee = pair.toSourceMoney(it.staticFee.toBigInteger()),
                    sampleDepositAddress = it.sampleDepositAddress,
                    expirationDate = it.expiresAt.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
                    creationDate = it.createdAt.fromIso8601ToUtc()?.toLocalTime() ?: Date()
                )
            }
        }
}