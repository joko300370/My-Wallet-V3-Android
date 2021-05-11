package info.blockchain.wallet.bip44

import info.blockchain.wallet.payload.data.Derivation
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.params.MainNetParams
import org.junit.Assert
import org.junit.Test

class HDChainTest {
    private val seed = "15e23aa73d25994f1921a1256f93f72c"
    private val key = HDKeyDerivation.createMasterPrivateKey(seed.toByteArray())

    @Test
    fun isReceive() {
        val chain1 = HDChain(MainNetParams.get(), key, true)
        Assert.assertTrue(chain1.isReceive)

        val chain2 = HDChain(MainNetParams.get(), key, false)
        Assert.assertFalse(chain2.isReceive)
    }

    @Test
    fun getAddressAt() {
        val chain = HDChain(MainNetParams.get(), key, true)
        Assert.assertEquals(
            "1HxBEXhu5LPibpTAQ1EoNTJavDSbwajJTg",
            chain.getAddressAt(0, Derivation.LEGACY_PURPOSE).formattedAddress
        )
    }

    @Test
    fun getSegwitAddressAt() {
        val chain = HDChain(MainNetParams.get(), key, true)
        Assert.assertEquals(
            "bc1qh8cka3lk4k74dnr7pqzyct8em57ky43a2x05lq",
            chain.getAddressAt(0, Derivation.SEGWIT_BECH32_PURPOSE).formattedAddress
        )
    }

    @Test
    fun getPath() {
        val chain = HDChain(MainNetParams.get(), key, true)
        Assert.assertEquals("M/0", chain.path)
    }
}