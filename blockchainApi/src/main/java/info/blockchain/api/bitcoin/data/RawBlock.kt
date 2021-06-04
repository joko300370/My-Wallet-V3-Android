package info.blockchain.api.bitcoin.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RawBlock(
    @SerialName("block_index")
    val blockIndex: Long = 0,
    @SerialName("hash")
    val hash: String? = null,
    @SerialName("height")
    val height: Long = 0,
    @SerialName("time")
    val time: Long = 0,
    @SerialName("ver")
    val ver: Long = 0,
    @SerialName("prev_block")
    val prevBlock: String? = null,
    @SerialName("mrkl_root")
    val merkleRoot: String? = null,
    @SerialName("bits")
    val bits: Long = 0,
    @SerialName("fee")
    val fee: Long = 0,
    @SerialName("nonce")
    val nonce: Long = 0,
    @SerialName("n_tx")
    val txCount: Long = 0,
    @SerialName("size")
    val size: Long = 0,
    @SerialName("main_chain")
    val isMainChain: Boolean = false,
    @SerialName("received_time")
    val receivedTime: Long = 0,
    @SerialName("relayed_by")
    val relayedBy: String? = null,
    @SerialName("tx")
    val tx: List<Transaction>? = null
)