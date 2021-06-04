package info.blockchain.wallet.payload.data

import info.blockchain.wallet.ImportedAddressHelper.getImportedAddress
import info.blockchain.wallet.WalletApiMockedResponseTest
import info.blockchain.wallet.crypto.AESUtil
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.exceptions.NoSuchAddressException
import info.blockchain.wallet.keys.SigningKeyImpl
import info.blockchain.wallet.payment.OutputType
import info.blockchain.wallet.payment.Payment
import info.blockchain.wallet.util.DoubleEncryptionFactory
import info.blockchain.wallet.util.parseUnspentOutputsAsUtxoList
import org.amshove.kluent.mock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.bitcoinj.core.Base58
import org.bitcoinj.core.ECKey
import org.bitcoinj.crypto.DeterministicKey
import org.json.JSONObject
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.spongycastle.util.encoders.Hex
import java.io.IOException
import java.math.BigInteger

/*
WalletBase
   |
   |__WalletWrapper
           |
           |__Wallet
*/

class WalletTest : WalletApiMockedResponseTest() {

    private fun givenWalletFromResource(resourceName: String, version: Int = 3): Wallet {
        return try {
            val mapper = WalletWrapper.getMapperForVersion(version)
            Wallet.fromJson(loadResourceContent(resourceName), mapper)
        } catch (e: HDWalletException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @Test
    fun fromJson_v4_1() {
        val resourceName = "wallet_body_v4_1.json"
        val wallet = givenWalletFromResource(resourceName, 4)
        assertEquals("a09910d9-1906-4ea1-a956-2508c3fe0661", wallet.guid)
        assertEquals("d14f3d2c-f883-40da-87e2-c8448521ee64", wallet.sharedKey)
        assertTrue(wallet.isDoubleEncryption)
        assertEquals("1f7cb884545e89e4083c10522bf8b991e8e13551aa5816110cb9419277fb4652", wallet.dpasswordhash)
        for ((key, value) in wallet.txNotes) {
            assertEquals("94a4934712fd40f2b91b7be256eacad49a50b850c949313b07046664d24c0e4c", key)
            assertEquals("Bought Pizza", value)
        }

        // Options parsing tested in OptionsTest
        assertNotNull(wallet.options)

        // HdWallets parsing tested in HdWalletsBodyTest
        assertNotNull(wallet.walletBodies)
        for (account in wallet.walletBody!!.accounts) {
            assertNotNull(account.label)
            assertNotNull(account.getDefaultXpub())
            assertNotNull(account.xpriv)
            assertNotNull(account.addressCache)
        }

        // Keys parsing tested in KeysBodyTest
        assertNotNull(wallet.importedAddressList)

        // AddressBook parsing tested in AddressBookTest
        assertNotNull(wallet.addressBook)
    }

    @Test
    fun fromJson_1() {
        val resourceName = "wallet_body_1.txt"
        val wallet = givenWalletFromResource(resourceName)
        assertEquals("a09910d9-1906-4ea1-a956-2508c3fe0661", wallet.guid)
        assertEquals("d14f3d2c-f883-40da-87e2-c8448521ee64", wallet.sharedKey)
        assertTrue(wallet.isDoubleEncryption)
        assertEquals("1f7cb884545e89e4083c10522bf8b991e8e13551aa5816110cb9419277fb4652", wallet.dpasswordhash)
        for ((key, value) in wallet.txNotes) {
            assertEquals("94a4934712fd40f2b91b7be256eacad49a50b850c949313b07046664d24c0e4c", key)
            assertEquals("Bought Pizza", value)
        }

        // Options parsing tested in OptionsTest
        assertNotNull(wallet.options)

        // HdWallets parsing tested in HdWalletsBodyTest
        assertNotNull(wallet.walletBodies)

        // Keys parsing tested in KeysBodyTest
        assertNotNull(wallet.importedAddressList)

        // AddressBook parsing tested in AddressBookTest
        assertNotNull(wallet.addressBook)
    }

    @Test
    fun fromJson_2() {
        val wallet = givenWalletFromResource("wallet_body_2.txt")
        assertEquals("9ebb4d4f-f36e-40d6-9a3e-5a3cca5f83d6", wallet.guid)
        assertEquals("41cf823f-2dcd-4967-88d1-ef9af8689fc6", wallet.sharedKey)
        assertFalse(wallet.isDoubleEncryption)
        assertNull(wallet.dpasswordhash)

        // Options parsing tested in OptionsTest
        assertNotNull(wallet.options)

        // Keys parsing tested in KeysBodyTest
        assertNotNull(wallet.importedAddressList)
    }

    @Test
    fun fromJson_3() {
        val wallet = givenWalletFromResource("wallet_body_3.txt")
        assertEquals("2ca9b0e4-6b82-4dae-9fef-e8b300c72aa2", wallet.guid)
        assertEquals("e8553981-b196-47cc-8858-5b0d16284f61", wallet.sharedKey)
        assertFalse(wallet.isDoubleEncryption)
        assertNull(wallet.dpasswordhash)

        // Options parsing tested in OptionsTest
        assertNotNull(wallet.walletOptions) // very old key for options
        assertEquals(10, wallet.walletOptions.pbkdf2Iterations.toLong())

        // old wallet_options should have created new options
        assertNotNull(wallet.options)
        assertEquals(10, wallet.options.pbkdf2Iterations.toLong())

        // Keys parsing tested in KeysBodyTest
        assertNotNull(wallet.importedAddressList)
    }

    @Test
    fun fromJson_4() {
        val wallet = givenWalletFromResource("wallet_body_4.txt")
        assertEquals("4077b6d9-73b3-4d22-96d4-9f8810fec435", wallet.guid)
        assertEquals("fa1beb37-5836-41d1-9f73-09f292076eb9", wallet.sharedKey)
    }

    @Test
    fun testToJSON() {
        // Ensure toJson doesn't write any unintended fields
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        val jsonString = wallet.toJson(WalletWrapper.getMapperForVersion(WalletWrapper.V3))
        val jsonObject = JSONObject(jsonString)
        assertEquals(10, jsonObject.keySet().size.toLong())
    }

    @Test
    fun validateSecondPassword() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        wallet.validateSecondPassword("hello")
        assertTrue(true)
    }

    @Test(expected = DecryptionException::class) @Throws(Exception::class) fun validateSecondPassword_fail() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        wallet.validateSecondPassword("bogus")
    }

    @Test @Throws(Exception::class)
    fun addAccount() {
        val wallet = givenWalletFromResource("wallet_body_6.txt")
        assertEquals(1, wallet.walletBody!!.accounts.size.toLong())

        wallet.addAccount("Some Label", null, 3)
        assertEquals(2, wallet.walletBody!!.accounts.size.toLong())

        val account = wallet.walletBody?.getAccount(wallet.walletBody!!.accounts.size - 1)
        assertEquals(
            "xpub6DTFzKMsjf1Tt9KwHMYnQxMLGuVRcobDZdz" +
                "Duhtc6xfvafsBFqsBS4RNM54kdJs9zK8RKkSbjSbwCeUJjxiySaBKTf8dmyXgUgVnFY7yS9x",
            account?.getDefaultXpub()
        )
        assertEquals(
            "xprv9zTuaopyuHTAffFUBL1n3pQbisewDLsNCR4d7KUzYd" +
                "8whsY2iJYvtG6tVp1c3jRU4euNj3qdb6wCrmCwg1JRPfPghmH3hJ5ubRJVmqMGwyy",
            account?.xpriv
        )
    }

    @Test(expected = DecryptionException::class)
    fun addAccount_doubleEncryptionError() {
        val wallet = givenWalletFromResource("wallet_body_6.txt")
        assertEquals(1, wallet.walletBody!!.accounts.size.toLong())
        wallet.addAccount("Some Label", "hello", 3)
    }

    @Test
    fun addAccount_doubleEncrypted() {
        val wallet = givenWalletFromResource("wallet_body_7.txt")
        assertEquals(2, wallet.walletBody!!.accounts.size.toLong())

        wallet.addAccount("Some Label", "hello", 3)
        assertEquals(3, wallet.walletBody!!.accounts.size.toLong())

        val account = wallet.walletBody?.getAccount(wallet.walletBody!!.accounts.size - 1)
        assertEquals(
            "xpub6DEe2bJAU7GbUw3HDGPUY9c77mUcP9xvAWEhx9GRe" +
                "uJM9gppeGxHqBcaYAfrsyY8R6cfVRsuFhi2PokQFYLEQBVpM8p4MTLzEHpVu4SWq9a",
            account?.getDefaultXpub()
        )

        // Private key will be encrypted
        val decryptedXpriv = DoubleEncryptionFactory.decrypt(
            account?.xpriv, wallet.sharedKey, "hello",
            wallet.options.pbkdf2Iterations
        )

        assertEquals(
            "xprv9zFHd5mGdjiJGSxp7ErUB1fNZje7yhF4oHK79krp6ZmNGt" +
                "Vg6je3HPJ6gueSWrVR9oqdqriu2DcshvTfSRu6PXyWiAbP8n6S7DVWEpu5kAE",
            decryptedXpriv
        )
    }

    @Test
    fun addLegacyAddress() {
        val wallet = givenWalletFromResource("wallet_body_6.txt")
        assertEquals(0, wallet.importedAddressList.size.toLong())

        mockInterceptor.setResponseString(
            "cb600366ef7a94b991aa04557fc1d9c272ba00df6b1d9791d71c66efa0ae7fe9"
        )
        wallet.addImportedAddress(getImportedAddress(), null)
        assertEquals(1, wallet.importedAddressList.size.toLong())

        val address = wallet.importedAddressList[wallet.importedAddressList.size - 1]

        assertNotNull(address.privateKey)
        assertNotNull(address.address)
        assertEquals("1", address.address.substring(0, 1))
    }

    @Test
    fun addLegacyAddress_doubleEncrypted() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        assertEquals(19, wallet.importedAddressList.size.toLong())
        mockInterceptor.setResponseString(
            "cb600366ef7a94b991aa04557fc1d9c272ba00df6b1d9791d71c66efa0ae7fe9"
        )

        wallet.addImportedAddress(getImportedAddress(), "hello")
        assertEquals(20, wallet.importedAddressList.size.toLong())

        val address = wallet.importedAddressList[wallet.importedAddressList.size - 1]
        assertNotNull(address.privateKey)
        assertNotNull(address.address)
        assertEquals("==", address.privateKey.substring(address.privateKey.length - 2))
        assertEquals("1", address.address.substring(0, 1))
    }

    @Test
    fun setKeyForLegacyAddress() {
        val wallet = givenWalletFromResource("wallet_body_6.txt")
        mockInterceptor.setResponseString(
            "cb600366ef7a94b991aa04557fc1d9c272ba00df6b1d9791d71c66efa0ae7fe9"
        )
        wallet.addImportedAddress(getImportedAddress(), null)
        val address = wallet.importedAddressList[wallet.importedAddressList.size - 1]
        val key = SigningKeyImpl(DeterministicKey.fromPrivate(Base58.decode(address.privateKey)))
        wallet.setKeyForImportedAddress(key, null)
    }

    @Test(expected = NoSuchAddressException::class)
    fun setKeyForLegacyAddress_NoSuchAddressException() {
        val wallet = givenWalletFromResource("wallet_body_6.txt")
        mockInterceptor.setResponseString(
            "cb600366ef7a94b991aa04557fc1d9c272ba00df6b1d9791d71c66efa0ae7fe9"
        )
        wallet.addImportedAddress(getImportedAddress(), null)

        // Try to set address key with ECKey not found in available addresses.
        val key = SigningKeyImpl(ECKey())
        wallet.setKeyForImportedAddress(key, null)
    }

    @Test
    fun setKeyForLegacyAddress_doubleEncrypted() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        mockInterceptor.setResponseString(
            "cb600366ef7a94b991aa04557fc1d9c272ba00df6b1d9791d71c66efa0ae7fe9"
        )
        wallet.addImportedAddress(getImportedAddress(), "hello")
        var address = wallet.importedAddressList[wallet.importedAddressList.size - 1]
        val decryptedOriginalPrivateKey = AESUtil
            .decrypt(
                address.privateKey, wallet.sharedKey + "hello",
                wallet.options.pbkdf2Iterations
            )

        // Remove private key so we can set it again
        address.privateKey = null

        // Same key for created address, but unencrypted
        val key = SigningKeyImpl(
            DeterministicKey.fromPrivate(Base58.decode(decryptedOriginalPrivateKey))
        )

        // Set private key
        wallet.setKeyForImportedAddress(key, "hello")

        // Get new set key
        address = wallet.importedAddressList[wallet.importedAddressList.size - 1]
        val decryptedSetPrivateKey = AESUtil
            .decrypt(
                address.privateKey, wallet.sharedKey + "hello",
                wallet.options.pbkdf2Iterations
            )

        // Original private key must match newly set private key (unencrypted)
        assertEquals(decryptedOriginalPrivateKey, decryptedSetPrivateKey)
    }

    @Test(expected = DecryptionException::class)
    fun setKeyForLegacyAddress_DecryptionException() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        mockInterceptor.setResponseString(
            "cb600366ef7a94b991aa04557fc1d9c272ba00df6b1d9791d71c66efa0ae7fe9"
        )
        wallet.addImportedAddress(getImportedAddress(), "hello")
        val address = wallet.importedAddressList[wallet.importedAddressList.size - 1]
        val decryptedOriginalPrivateKey = AESUtil
            .decrypt(
                address.privateKey, wallet.sharedKey + "hello",
                wallet.options.pbkdf2Iterations
            )

        // Remove private key so we can set it again
        address.privateKey = null

        // Same key for created address, but unencrypted
        val key = SigningKeyImpl(
            DeterministicKey.fromPrivate(Base58.decode(decryptedOriginalPrivateKey))
        )

        // Set private key
        wallet.setKeyForImportedAddress(key, "bogus")
    }

    @Test
    fun decryptHDWallet() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        wallet.decryptHDWallet("hello")
    }

    @Test(expected = DecryptionException::class)
    fun decryptHDWallet_DecryptionException() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        wallet.decryptHDWallet("bogus")
    }

    @Test
    fun getMasterKey() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        wallet.decryptHDWallet("hello")
        assertEquals(
            "4NPYyXS5fhyoTHgDPt81cQ4838j1tRwmeRbK8pGLB1Xg",
            Base58.encode(wallet.walletBody!!.masterKey.toDeterministicKey().privKeyBytes)
        )
    }

    @Test
    fun getHdSeed() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        wallet.decryptHDWallet("hello")
        val seedAccess = wallet.walletBody
        assertEquals(
            "a55d76ccbd8a996fc3ae734db75aacf7cfa6d52f8e9e279" +
                "2bbbdbd54ba14fae6a24f34a90f635cdb70b544dd65828cac857de70d6aacda09fa082ed4632e7ce0",
            Hex.toHexString(seedAccess!!.hdSeed)
        )
    }

    @Test(expected = DecryptionException::class)
    fun getMasterKey_DecryptionException() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        wallet.decryptHDWallet("bogus")
        wallet.walletBody!!.masterKey
    }

    @Test
    fun getMnemonic() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        wallet.decryptHDWallet("hello")
        assertEquals(
            "[car, region, outdoor, punch, poverty, shadow, insane, claim, one, whisper, learn, alert]",
            wallet.walletBody!!.mnemonic.toString()
        )
    }

    @Test(expected = DecryptionException::class)
    fun getMnemonic_DecryptionException() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")
        wallet.decryptHDWallet("bogus")
        wallet.walletBody!!.mnemonic
    }

    @Test
    fun hDKeysForSigning() {
        val wallet = givenWalletFromResource("wallet_body_1.txt")

        // Available unspents: [8290, 4616, 5860, 3784, 2290, 13990, 8141]
        val resource = loadResourceContent("wallet_body_1_account1_unspent.txt")
        val unspentOutputs = parseUnspentOutputsAsUtxoList(resource)

        val payment = Payment(bitcoinApi = mock())
        val spendAmount: Long = 40108
        val paymentBundle = payment
            .getSpendableCoins(
                unspentOutputs,
                OutputType.P2PKH,
                OutputType.P2PKH,
                BigInteger.valueOf(spendAmount),
                BigInteger.valueOf(1000L),
                false
            )

        assertEquals(789, paymentBundle.absoluteFee.toLong())
        wallet.decryptHDWallet("hello")
        val keyList = wallet.walletBody?.getHDKeysForSigning(
            wallet.walletBody!!.getAccount(0), paymentBundle
        )

        // Contains 5 matching keys for signing
        assertEquals(5, keyList?.size)
    }

    @Test
    fun createNewWallet() {
        val label = "HDAccount 1"
        val payload = Wallet(label, true)
        assertEquals(36, payload.guid.length.toLong()) // GUIDs are 36 in length
        assertEquals(label, payload.walletBody!!.accounts[0].label)
        assertEquals(1, payload.walletBody!!.accounts.size.toLong())
        assertEquals(5000, payload.options.pbkdf2Iterations.toLong())
        assertEquals(600000, payload.options.logoutTime)
        assertEquals(10000, payload.options.feePerKb)
    }
}

private fun Account.getDefaultXpub() =
    xpubs.default.address
