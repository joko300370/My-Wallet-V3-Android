package com.blockchain.swap.nabu.datamanagers.repositories

import com.blockchain.swap.nabu.Authenticator
import com.blockchain.swap.nabu.datamanagers.Direction
import com.blockchain.swap.nabu.datamanagers.PriceTier
import com.blockchain.swap.nabu.datamanagers.SwapQuote
import com.blockchain.swap.nabu.extensions.fromIso8601ToUtc
import com.blockchain.swap.nabu.extensions.toLocalTime
import com.blockchain.swap.nabu.models.swap.QuoteRequest
import com.blockchain.swap.nabu.service.NabuService
import java.util.Date

class QuotesProvider(
    private val nabuService: NabuService,
    private val authenticator: Authenticator
) {

    fun fetchQuote(product: String = "BROKERAGE", direction: Direction, pair: String) =
        authenticator.authenticate { sessionToken ->
            nabuService.fetchQuote(sessionToken,
                QuoteRequest(
                    product = product,
                    direction = direction.toString(),
                    pair = pair
                )).map {
                SwapQuote(
                    id = it.id,
                    prices = it.quote.priceTiers.map { price ->
                        PriceTier(volume = price.volume.toBigDecimal(),
                            price = price.price.toBigDecimal(),
                            marginPrice = price.marginPrice.toBigDecimal())
                    },
                    sampleDepositAddress = it.sampleDepositAddress,
                    expirationDate = it.expiresAt.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
                    creationDate = it.createdAt.fromIso8601ToUtc()?.toLocalTime() ?: Date()
                )
            }
        }
}