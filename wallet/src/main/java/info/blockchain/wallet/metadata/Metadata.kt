package info.blockchain.wallet.metadata

import com.google.common.annotations.VisibleForTesting
import info.blockchain.wallet.util.MetadataUtil
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.crypto.DeterministicKey

class Metadata(
    @VisibleForTesting
    val address: String,
    val node: ECKey,
    val encryptionKey: ByteArray? = null,
    val isEncrypted: Boolean = true,
    val type: Int = 0
) {
    companion object {
        fun newInstance(
            metaDataHDNode: DeterministicKey,
            type: Int = -1,
            isEncrypted: Boolean = true,
            encryptionKey: ByteArray? = null,
            metadataDerivation: MetadataDerivation
        ): Metadata {

            val payloadTypeNode = MetadataUtil.deriveHardened(metaDataHDNode, type)
            val newNode = MetadataUtil.deriveHardened(payloadTypeNode, 0)

            val address = metadataDerivation.deriveAddress(newNode)
            return Metadata(
                address = address,
                node = newNode,
                encryptionKey = encryptionKey ?: Sha256Hash.hash(
                    MetadataUtil.deriveHardened(payloadTypeNode, 1).privKeyBytes
                ),
                isEncrypted = isEncrypted,
                type = type
            )
        }
    }
}