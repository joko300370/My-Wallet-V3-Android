package info.blockchain.api.bitcoin.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class XpubDto(
    @SerialName("m")
    val address: String,
    @SerialName("path")
    val derivationPath: String
)