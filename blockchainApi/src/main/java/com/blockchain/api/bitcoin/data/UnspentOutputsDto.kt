package com.blockchain.api.bitcoin.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UnspentOutputsDto(
    @SerialName("notice")
    val notice: String? = null,
    @SerialName("unspent_outputs")
    val unspentOutputs: List<UnspentOutputDto> = emptyList()
)