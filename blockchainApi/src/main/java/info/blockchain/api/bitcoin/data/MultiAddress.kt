package info.blockchain.api.bitcoin.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MultiAddress(
    @SerialName("wallet")
    val multiAddressBalance: MultiAddressBalance,
    @SerialName("addresses")
    val addresses: List<AddressSummary> = emptyList(),
    @SerialName("txs")
    val txs: List<Transaction> = emptyList(),
    @SerialName("info")
    val info: Info
)