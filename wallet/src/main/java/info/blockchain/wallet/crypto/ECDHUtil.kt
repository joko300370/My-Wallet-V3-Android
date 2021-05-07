package info.blockchain.wallet.crypto

import com.blockchain.preferences.BrowserIdentity
import info.blockchain.wallet.keys.SigningKey
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash
import org.spongycastle.crypto.digests.SHA256Digest
import org.spongycastle.crypto.generators.HKDFBytesGenerator
import org.spongycastle.crypto.params.HKDFParameters
import org.spongycastle.util.encoders.Hex
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object ECDHUtil {

    fun getPublicKeyAsHexString(signingKey: SigningKey): String = Hex.toHexString(signingKey.toECKey().pubKey)

    fun getEncryptedMessage(
        signingKey: SigningKey,
        browserIdentity: BrowserIdentity,
        serializedMessage: String
    ): String {
        val channelKey = getSharedKey(signingKey, browserIdentity)
        val encrypted = encrypt(serializedMessage.toByteArray(), channelKey)
        return Hex.toHexString(encrypted)
    }

    fun getDecryptedMessage(
        signingKey: SigningKey,
        browserIdentity: BrowserIdentity,
        encryptedMessage: String
    ): ByteArray {
        val channelKey = getSharedKey(signingKey, browserIdentity)
        return decrypt(Hex.decode(encryptedMessage), channelKey)
    }

    fun getSharedKey(key: SigningKey, browserIdentity: BrowserIdentity): ByteArray {
        val private: ByteArray = key.toECKey().privKeyBytes
        val public: ByteArray = Hex.decode(browserIdentity.pubkey)
        val privateBigInt = ECKey.fromPrivate(private).privKey
        val ecKey = ECKey.fromPublicOnly(public)

        val multiplied = ecKey.pubKeyPoint.multiply(privateBigInt)

        // As per https://crypto.stackexchange.com/questions/60515/ecdh-over-secp256k1-implementation
        // it is common to include the X prefix into the preimage within the Bitcoin ecosystem
        // Further, the secp256k1 library that we use, hashes the x coordinate. For the point of
        // not having to roll our own on iOS, let's replicate this here. We will pass the final result
        // into a HKDF function afterwards anyways.
        val encoded = multiplied.getEncoded(true)
        val hashed = Sha256Hash.of(encoded).bytes

        val hkdf = HKDFBytesGenerator(SHA256Digest())
        hkdf.init(HKDFParameters(hashed, ByteArray(0), ByteArray(0)))

        val okm = ByteArray(32)
        hkdf.generateBytes(okm, 0, 32)

        return okm
    }

    // https://proandroiddev.com/security-best-practices-symmetric-encryption-with-aes-in-java-7616beaaade9
    fun encrypt(plaintext: ByteArray, keyRaw: ByteArray): ByteArray {
        val key = SecretKeySpec(keyRaw, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        val secureRandom = SecureRandom()

        val iv = ByteArray(12)
        secureRandom.nextBytes(iv)
        val parameterSpec = GCMParameterSpec(128, iv)

        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec)
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext
    }

    fun decrypt(ciphertextRaw: ByteArray, keyRaw: ByteArray): ByteArray {
        val key = SecretKeySpec(keyRaw, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ciphertextRaw.take(12).toByteArray()
        val ciphertext = ciphertextRaw.drop(12).toByteArray()
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        return cipher.doFinal(ciphertext)
    }

    fun hkdf(bytes: ByteArray): ByteArray {
        val generator = HKDFBytesGenerator(SHA256Digest())
        generator.init(HKDFParameters.defaultParameters(bytes))
        val output = ByteArray(32)
        generator.digest.doFinal(output, 0)
        return output
    }
}