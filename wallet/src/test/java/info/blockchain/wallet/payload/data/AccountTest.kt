package info.blockchain.wallet.payload.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.bitcoinj.crypto.HDKeyDerivation
import org.junit.Before
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

class AccountTest {
    @Before
    fun setup() {
        val seed = "15e23aa73d25994f1921a1256f93f72c"
        val key = HDKeyDerivation.createMasterPrivateKey(seed.toByteArray())
    }

    @Test
    fun fromJson_1() {
        val uri = javaClass.classLoader.getResource("wallet_body_1.txt")?.toURI()!!
        val body = String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8)
        val wallet = Wallet.fromJson(body)
        val hdWallet = wallet.walletBody
        val accounts = hdWallet!!.accounts
        assertEquals(68, accounts.size.toLong())
        assertEquals("My Wallet", accounts[0].label)
        assertFalse(accounts[0].isArchived)
        assertEquals(
            "FhyK+vchEiRwTFmIFye+V182e38sNjStJGT/eFcy7TKyrfGh+Xnfa" +
                "Ji90IRHatMJVCvIo7jZApX7x3zc87UzqR8tH2yIpqIcjR3IXWA7IGXfi9grN" +
                "c6UU3tF5BaYV/jlrAOij/7mGsZSYam5G5Fz8gscVYf4+4ZC9eM+q80lN1Q=",
            accounts[0].xpriv
        )
        assertEquals(
            "xpub6DEe2bJAU7GbP12FBdsBckUkGPzQKMnZXaF2ajz2NCFf" +
                "YJMEzb5G3oGwYrE6WQjnjhLeB6TgVudV3B9kKtpQmYeBJZLRNyXCobPht2jPUBm",
            accounts[0].getDefaultXpub()
        )
        assertNotNull(accounts[0].addressCache)
        assertNotNull(accounts[0].addressLabels)
        assertEquals("Savings 1", accounts[1].label)
        assertTrue(accounts[1].isArchived)
        assertEquals(
            "nz3xhp6xFfBxOe4+l1xLePSdL5E4cBDCj/TC1nNNSokvHZT" +
                "dXgbuTV5Ow+Gh7ZbOpth3Oh2iZrCibpwgiiler0A/TKDu++V1Qnu" +
                "JOmK/77WGYizm/e563eultBUuQCNktEfNGQUVveCYeF+TfsTU24tS3xbKzK4JeYiXaVlN4fk=",
            accounts[1].xpriv
        )
        assertEquals(
            "xpub6DEe2bJAU7GbQcGHvqgJ4T6pzZUU8j1WqLPyVtaWJFewfj" +
                "ChAKtUX5uRza9rabc6rAgFhXptveBmaoy7ptVGgbYT8KKaJ9E7wmyj5o4aqvr",
            accounts[1].getDefaultXpub()
        )
        assertNotNull(accounts[1].addressCache)

        // AddressLabel parsing tested in AddressLabelTest
        assertNotNull(accounts[1].addressLabels)
    }

    @Test
    fun fromJson_6() {
        val uri = javaClass.classLoader.getResource("wallet_body_6.txt")?.toURI()!!
        val body = String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8)
        val wallet = Wallet.fromJson(body)
        val hdWallet = wallet.walletBody
        val accounts = hdWallet!!.accounts
        assertEquals(1, accounts.size.toLong())
        assertEquals("My Bitcoin Wallet", accounts[0].label)
        assertFalse(accounts[0].isArchived)
        assertEquals(
            "xprv9xvLaqAsee2mgFsgMQsVLCTh858tA559kD9wczD5n" +
                "YGJMa4M56MvLgYGGn75MSdDFZSBYeYeCgAZdqKQitXux3ebiTi67eYH1a1VS2rdKZW",
            accounts[0].xpriv
        )
        assertEquals(
            "xpub6BugzLhmV1b4tjx9TSQVhLQRg6yNZXo17S5YR" +
                "NchLsoHENPVcdgAtUrk82X5LNuaViWoxsqMhCd3UBxhQRHvyrUeqqA7tupvSpkoC73nhL1",
            accounts[0].getDefaultXpub()
        )
        assertNotNull(accounts[0].addressCache)
        assertNotNull(accounts[0].addressLabels)
    }
}

private fun Account.getDefaultXpub() =
    xpubs.default.address