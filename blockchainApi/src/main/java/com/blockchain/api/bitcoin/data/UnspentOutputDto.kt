package com.blockchain.api.bitcoin.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UnspentOutputDto(
    @SerialName("tx_age")
    private val txAge: Long = 0,
    @SerialName("tx_hash")
    val txHash: String,
    @SerialName("tx_hash_big_endian")
    val txHashBigEndian: String? = null,
    @SerialName("tx_index")
    val txIndex: Long = 0,
    @SerialName("tx_output_n")
    val txOutputCount: Int = 0,
    @SerialName("script")
    val script: String? = null,
    @SerialName("value")
    val value: String? = null,
    @SerialName("value_hex")
    val valueHex: String? = null,
    @SerialName("confirmations")
    val confirmations: Long = 0,
    @SerialName("xpub")
    val xpub: XpubDto? = null,
    @SerialName("replayable")
    val replayable: Boolean = true
)