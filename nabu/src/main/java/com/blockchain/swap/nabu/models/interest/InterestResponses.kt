package com.blockchain.swap.nabu.models.interest

import com.squareup.moshi.Json

data class InterestResponse(
    val rate: Double
)

@Json(name = "BTC")
data class InterestAccountBalanceResponse(
    val balance: Long
)