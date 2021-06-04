package info.blockchain.wallet.crypto

import com.blockchain.preferences.BrowserIdentity
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.keys.SigningKeyImpl
import junit.framework.Assert.assertEquals
import org.bitcoinj.core.ECKey
import org.junit.Test
import org.spongycastle.util.encoders.Hex
import java.nio.charset.Charset
import java.security.SecureRandom
import javax.crypto.AEADBadTagException

class ECDHUtilTest {

    private val random = SecureRandom()

    @Test
    fun `should encrypt and decrypt`() {
        val secret = ByteArray(32)
        random.nextBytes(secret)

        val clearText = "This is a test sentence!"

        val cipherText = ECDHUtil.encrypt(clearText.toByteArray(Charset.defaultCharset()), secret)
        assert(!clearText.toByteArray(Charset.defaultCharset()).contentEquals(cipherText))

        val decrypted = ECDHUtil.decrypt(cipherText, secret)
        assert(decrypted.toString(Charset.defaultCharset()) == clearText)
    }

    @Test
    fun `should fail to decrypt modified message`() {
        val secret = ByteArray(32)
        random.nextBytes(secret)

        val clearText = "This is a test sentence!"

        val cipherText = ECDHUtil.encrypt(clearText.toByteArray(Charset.defaultCharset()), secret)
        assert(!clearText.toByteArray(Charset.defaultCharset()).contentEquals(cipherText))

        repeat(cipherText.size) {
            val copy = cipherText.copyOf()
            copy[it]++

            try {
                ECDHUtil.decrypt(copy, secret)
                assert(false)
            } catch (e: AEADBadTagException) { }
        }
    }

    @Test
    fun `should correctly derive shared secret`() {
        val key1 = SigningKeyImpl(ECKey())
        val key2 = SigningKeyImpl(ECKey())
        val browserIdentity = BrowserIdentity(key1.toECKey().publicKeyAsHex)

        val shared1 = ECDHUtil.getSharedKey(key1, browserIdentity)
        val shared2 = ECDHUtil.getSharedKey(key1, browserIdentity)
        assert(shared1.contentEquals(shared2))
    }

    // These tests are to make sure the different platforms derive the same key
    @Test
    fun `should correctly derive from test vectors`() {
        val signingKey = SigningKey.createSigningKeyFromPrivateKey(
            "9cd3b16e10bd574fed3743d8e0de0b7b4e6c69f3245ab5a168ef010d22bfefa0"
        )
        val browserIdentity = BrowserIdentity(
            "02a18a98316b5f52596e75bfa5ca9fa9912edd0c989b86b73d41bb64c9c6adb992"
        )
        val shared = ECDHUtil.getSharedKey(
            signingKey,
            browserIdentity
        )

        assertEquals(Hex.toHexString(shared), "c87e593a1b22bad696489aa7c240356ffc8ff453d4637dc9cd32b4696df93f5c")
    }

    @Test
    fun `should correctly decrypt from test vectors`() {
        val decrypted = ECDHUtil.decrypt(
                Hex.decode(
                    "83e77704adf28646b602047763a179b5991a5d5d4457658200" +
                    "c84936c71e5e7ffb54a1dcf665d836cb2ce34a471747eb64392e80"),
                Hex.decode("9cd3b16e10bd574fed3743d8e0de0b7b4e6c69f3245ab5a168ef010d22bfefa0")
        )
        assertEquals("This is a test sentence!", decrypted.toString(Charset.defaultCharset()))
    }
}