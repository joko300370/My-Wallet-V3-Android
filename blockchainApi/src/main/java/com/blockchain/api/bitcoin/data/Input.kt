package com.blockchain.api.bitcoin.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Input(
    @SerialName("sequence")
    val sequence: Long = 0,
    @SerialName("prev_out")
    val prevOut: Output? = null,
    @SerialName("script")
    val script: String? = null
)