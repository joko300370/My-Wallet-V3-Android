package com.blockchain.api.bitcoin.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Info(
    @SerialName("nconnected")
    val connectedCount: Long = 0,
    @SerialName("conversion")
    val conversion: Double = 0.0,
    @SerialName("latest_block")
    val latestBlock: RawBlock
)