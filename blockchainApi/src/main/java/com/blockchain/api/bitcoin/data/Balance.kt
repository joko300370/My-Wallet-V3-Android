package com.blockchain.api.bitcoin.data

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class BalanceDto(
    @SerialName("final_balance")
    val finalBalance: String,
    @SerialName("n_tx")
    val txCount: Long = 0,
    @SerialName("total_received")
    val totalReceived: String
)

typealias BalanceResponseDto = Map<String, BalanceDto>