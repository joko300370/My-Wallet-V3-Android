package info.blockchain.wallet.metadata

import com.google.common.annotations.VisibleForTesting
import info.blockchain.wallet.util.MetadataUtil
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.crypto.DeterministicKey
import java.util.Arrays

class Metadata(
    @VisibleForTesting
    val address: String,
    val node: ECKey,
    val encryptionKey: ByteArray,
    val unpaddedEncryptionKey: ByteArray? = null,
    val type: Int = 0
) {
    companion object {
        fun newInstance(
            metaDataHDNode: DeterministicKey,
            type: Int = -1,
            metadataDerivation: MetadataDerivation
        ): Metadata {
            val payloadTypeNode = MetadataUtil.deriveHardened(metaDataHDNode, type)
            val newNode = MetadataUtil.deriveHardened(payloadTypeNode, 0)

            val keyBytes = MetadataUtil.deriveHardened(payloadTypeNode, 1).privKeyBytes

            // Fix for inter-op decryption issue, trim leading zero bytes from the key to
            // generate a second, backup, decryption key
            val firstNonZeroByte = keyBytes.indexOfFirst { b -> b != 0.toByte() }
            val keyBytesPatch = if (firstNonZeroByte != 0) {
                Arrays.copyOfRange(keyBytes, firstNonZeroByte, keyBytes.size - 1)
            } else { null }

            val address = metadataDerivation.deriveAddress(newNode)
            return Metadata(
                address = address,
                node = newNode,
                encryptionKey = Sha256Hash.hash(keyBytes),
                unpaddedEncryptionKey = keyBytesPatch?.let { Sha256Hash.hash(it) },
                type = type
            )
        }
    }
}