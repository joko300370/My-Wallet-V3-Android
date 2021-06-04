@file:UseSerializers(BigIntSerializer::class)
package info.blockchain.api.bitcoin.data

import info.blockchain.api.serializers.BigIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.math.BigInteger

@Serializable
data class AddressSummary(
    @SerialName("address")
    val address: String,
    @SerialName("n_tx")
    val txCount: Long = 0,
    @SerialName("total_received")
    val totalReceived: BigInteger,
    @SerialName("total_sent")
    val totalSent: BigInteger,
    @SerialName("final_balance")
    val finalBalance: BigInteger,
    @SerialName("change_index")
    val changeIndex: Int = 0,
    @SerialName("account_index")
    val accountIndex: Int = 0,
    @SerialName("gap_limit")
    private val gapLimit: Long = 0
)
