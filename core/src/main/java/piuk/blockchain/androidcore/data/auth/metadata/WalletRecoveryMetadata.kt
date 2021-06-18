package piuk.blockchain.androidcore.data.auth.metadata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WalletRecoveryMetadata(
    @SerialName("guid") val guid: String,
    @SerialName("password") val password: String,
    @SerialName("sharedKey") val sharedKey: String
) {

    companion object {
        const val WALLET_CREDENTIALS_METADATA_NODE = 12
    }
}