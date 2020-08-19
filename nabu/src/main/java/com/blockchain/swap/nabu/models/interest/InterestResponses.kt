package com.blockchain.swap.nabu.models.interest

data class InterestResponse(
    val rate: Double
)

data class InterestAccountBalanceResponse(
    val balance: String
)

data class InterestAddressResponse(
    val accountRef: String
)