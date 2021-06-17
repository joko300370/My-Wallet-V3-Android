@file:UseSerializers(BigIntSerializer::class)
package com.blockchain.api.bitcoin.data

import com.blockchain.api.serializers.BigIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.math.BigInteger

@Serializable
data class Transaction(
    @SerialName("hash")
    val hash: String? = null,
    @SerialName("ver")
    val ver: Long = 0,
    @SerialName("lock_time")
    val lockTime: Long = 0,
    @SerialName("block_height")
    val blockHeight: Long? = 0,
    @SerialName("relayed_by")
    val relayedBy: String? = null,
    @SerialName("result")
    val result: BigInteger,
    @SerialName("fee")
    val fee: BigInteger? = null,
    @SerialName("size")
    val size: Long = 0,
    @SerialName("time")
    val time: Long = 0,
    @SerialName("tx_index")
    val txIndex: Long = 0,
    @SerialName("vin_sz")
    val vinSz: Long = 0,
    @SerialName("vout_sz")
    val voutSz: Long = 0,
    @SerialName("double_spend")
    val isDoubleSpend: Boolean = false,
    @SerialName("inputs")
    val inputs: List<Input> = emptyList(),
    @SerialName("out")
    val out: List<Output> = emptyList()
)