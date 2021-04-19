package info.blockchain.wallet.metadata

import info.blockchain.wallet.keys.MasterKeyImpl
import org.bitcoinj.crypto.HDKeyDerivation
import org.junit.Assert
import org.junit.Test

class MetadataDerivationTest {

    private val metadataDerivation = MetadataDerivation()
    private val seed = "15e23aa73d25994f1921a1256f93f72c"
    val key = HDKeyDerivation.createMasterPrivateKey(seed.toByteArray())

    @Test
    fun `derive metadata node from determenistic key`() {
        Assert.assertEquals("xprv9v8qUhWNur6b9if9MZhw1hvxnsWgonfw" +
                "9dv1hzVAoCeVpSAWrXd7woo27QqegYXnmYYTDgYjFJTYXTvAu6ZZnS6P7SSkTZTurJ5STEb7952",
            metadataDerivation.deriveMetadataNode(MasterKeyImpl(key)))
    }

    @Test
    fun `derive shared metadata node from determenistic key`() {
        Assert.assertEquals("xprv9v8qUhWTVjGx3vstKw6J8HV4CNmZB1oTPJGzoyYz9ZK2Y" +
                "cNviZesiy4KhmgyPF635czS66bL8iANAoJbheDbMV7V41Lc9TYg43AE6vF2pFT",
            metadataDerivation.deriveSharedMetadataNode(MasterKeyImpl(key)))
    }
}