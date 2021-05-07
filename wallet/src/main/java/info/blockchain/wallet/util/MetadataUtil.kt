package info.blockchain.wallet.util

import com.google.common.base.Charsets
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Utils
import org.bitcoinj.core.VarInt
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Arrays

object MetadataUtil {

    @Throws(UnsupportedEncodingException::class, NoSuchAlgorithmException::class)
    fun deriveMetadataNode(node: DeterministicKey): DeterministicKey {
        return HDKeyDerivation.deriveChildKey(
            node,
            getPurpose("metadata") or ChildNumber.HARDENED_BIT
        )
    }

    @Throws(IOException::class)
    fun message(payload: ByteArray, prevMagicHash: ByteArray?): ByteArray {
        return if (prevMagicHash == null)
            payload
        else {
            val payloadHash = Sha256Hash.hash(payload)

            val outputStream = ByteArrayOutputStream()
            outputStream.write(prevMagicHash)
            outputStream.write(payloadHash)

            outputStream.toByteArray()
        }
    }

    fun magic(payload: ByteArray, prevMagicHash: ByteArray?): ByteArray {
        val msg = message(payload, prevMagicHash)
        return magicHash(msg)
    }

    private fun magicHash(message: ByteArray): ByteArray {
        val messageBytes = formatMessageForSigning(Base64Util.encodeBase64String(message))
        return Sha256Hash.hashTwice(messageBytes)
    }

    /** The string that prefixes all text messages signed using Bitcoin keys.  */
    private const val BITCOIN_SIGNED_MESSAGE_HEADER = "Bitcoin Signed Message:\n"
    private val BITCOIN_SIGNED_MESSAGE_HEADER_BYTES = BITCOIN_SIGNED_MESSAGE_HEADER.toByteArray(Charsets.UTF_8)

    // public static byte[] formatMessageForSigning(String message)
    // Given a textual message, returns a byte buffer formatted as follows:
    //
    // [24] "Bitcoin Signed Message:\n" [message.length as a varint] message
    // (This maybe exists in the newer bitcoinj lib, but I can't find it so:
    private fun formatMessageForSigning(message: String): ByteArray? {
        return try {
            val bos = ByteArrayOutputStream()
            bos.write(BITCOIN_SIGNED_MESSAGE_HEADER_BYTES.size)
            bos.write(BITCOIN_SIGNED_MESSAGE_HEADER_BYTES)
            val messageBytes = message.toByteArray(Charsets.UTF_8)
            val size = VarInt(messageBytes.size.toLong())
            bos.write(size.encode())
            bos.write(messageBytes)
            bos.toByteArray()
        } catch (e: IOException) {
            throw RuntimeException(e) // Cannot happen.
        }
    }

    fun deriveHardened(node: DeterministicKey, type: Int): DeterministicKey {
        return HDKeyDerivation.deriveChildKey(node, type or ChildNumber.HARDENED_BIT)
    }

    /**
     * BIP 43 purpose needs to be 31 bit or less. For lack of a BIP number we take the first 31 bits
     * of the SHA256 hash of a reverse domain.
     */
    @Throws(NoSuchAlgorithmException::class, UnsupportedEncodingException::class)
    private fun getPurpose(sub: String): Int {

        val md = MessageDigest.getInstance("SHA-256")
        val text = "info.blockchain.$sub"
        md.update(text.toByteArray(charset("UTF-8")))
        val hash = md.digest()
        val slice = Arrays.copyOfRange(hash, 0, 4)

        return (Utils.readUint32BE(slice, 0) and 0x7FFFFFFF).toInt() // 510742
    }
}
