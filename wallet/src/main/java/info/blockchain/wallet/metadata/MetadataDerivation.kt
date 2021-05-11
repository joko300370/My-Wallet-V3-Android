package info.blockchain.wallet.metadata

import info.blockchain.wallet.keys.MasterKey
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.core.Utils
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.params.MainNetParams
import java.security.MessageDigest
import java.util.Arrays

class MetadataDerivation {

    fun deriveMetadataNode(node: MasterKey): String {
        return HDKeyDerivation.deriveChildKey(
            node.toDeterministicKey(),
            getPurpose("metadata") or ChildNumber.HARDENED_BIT
        ).serializePrivB58(MainNetParams.get())
    }

    fun deriveSharedMetadataNode(node: MasterKey): String {
        return HDKeyDerivation.deriveChildKey(
            node.toDeterministicKey(),
            getPurpose("mdid") or ChildNumber.HARDENED_BIT).serializePrivB58(MainNetParams.get()
        )
    }

    fun deriveAddress(key: ECKey): String = LegacyAddress.fromKey(MainNetParams.get(), key).toString()

    /**
     * BIP 43 purpose needs to be 31 bit or less. For lack of a BIP number we take the first 31 bits
     * of the SHA256 hash of a reverse domain.
     */
    private fun getPurpose(sub: String): Int {

        val md = MessageDigest.getInstance("SHA-256")
        val text = "info.blockchain.$sub"
        md.update(text.toByteArray(charset("UTF-8")))
        val hash = md.digest()
        val slice = Arrays.copyOfRange(hash, 0, 4)

        return (Utils.readUint32BE(slice, 0) and 0x7FFFFFFF).toInt() // 510742
    }

    fun deserializeMetadataNode(node: String): DeterministicKey =
        DeterministicKey.deserializeB58(
            node,
            MainNetParams.get()
        )
}