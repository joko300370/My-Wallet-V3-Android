package com.blockchain.swap.nabu.models.swap

class QuoteRequest(
    private val product: String,
    private val direction: String,
    private val pair: String
)

class QuoteResponse(
    val id: String,
    val product: String,
    val pair: String,
    val quote: Quote,
    val createdAt: String,
    val sampleDepositAddress: String,
    val expiresAt: String
)

class SwapOrderResponse(
    val id: String,
    val state: String,
    val kind: OrderKind
) {
    companion object {
        const val PENDING_EXECUTION = "PENDING_EXECUTION"
        const val PENDING_DEPOSIT = "PENDING_DEPOSIT"
        const val FINISH_DEPOSIT = "FINISH_DEPOSIT"
        const val PENDING_WITHDRAWAL = "PENDING_WITHDRAWAL"
        const val FAILED = "FAILED"
        const val FINISHED = "FINISHED"
        const val EXPIRED = "EXPIRED"
    }
}

class OrderKind(
    val depositAddress: String?,
    val withdrawalAddress: String?
)

class Quote(val priceTiers: List<InterpolationPrices>)

data class InterpolationPrices(
    val volume: String,
    val price: String,
    val marginPrice: String
)

class PriceFunnel(
    val inputMoney: String,
    val price: String,
    val networkFee: String,
    val staticFee: String,
    val outputMoney: String
)

class CreateOrderRequest(
    private val direction: String,
    private val quoteId: String,
    private val volume: String,
    private val destinationAddress: String? = null
)

data class SwapLimitsResponse(
    val currency: String,
    val minOrder: String,
    val maxOrder: String,
    val maxPossibleOrder: String,
    val daily: TimeLimitsResponse,
    val weekly: TimeLimitsResponse,
    val annual: TimeLimitsResponse
)

data class TimeLimitsResponse(
    val limit: String,
    val available: String,
    val used: String
)