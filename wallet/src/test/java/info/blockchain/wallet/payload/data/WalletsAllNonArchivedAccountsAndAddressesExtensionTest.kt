package info.blockchain.wallet.payload.data

import org.amshove.kluent.`should equal`
import org.junit.Test

class WalletsAllNonArchivedAccountsAndAddressesExtensionTest {

    private fun importedAddressWithPrivateKey(address: String) =
        ImportedAddress().also {
            it.privateKey = "PRIVATE_KEY"
            it.address = address
        }

    @Test
    fun `empty list`() {
        Wallet().allNonArchivedAccountsAndAddresses() `should equal` emptyList()
    }

    @Test
    fun `one spendable`() {
        val wallet = Wallet().apply {
            importedAddressList.add(importedAddressWithPrivateKey("Address1"))
        }

        val result = wallet.allNonArchivedAccountsAndAddresses()
        result `should equal` listOf("Address1")
    }

    @Test
    fun `one archived`() {
        Wallet().apply {
            importedAddressList.add(importedAddressWithPrivateKey("Address1").apply { archive() })
        }.allNonArchivedAccountsAndAddresses() `should equal` emptyList()
    }

    @Test
    fun `one without private key`() {
        Wallet().apply {
            importedAddressList.add(ImportedAddress().apply {
                address = "Address1"
            })
        }.allNonArchivedAccountsAndAddresses() `should equal` listOf("Address1")
    }

    @Test
    fun `two spendable`() {
        Wallet().apply {
            importedAddressList.add(importedAddressWithPrivateKey("Address1"))
            importedAddressList.add(importedAddressWithPrivateKey("Address2"))
        }.allNonArchivedAccountsAndAddresses() `should equal` listOf("Address1", "Address2")
    }

    @Test
    fun `repeated address`() {
        Wallet().apply {
            importedAddressList.add(importedAddressWithPrivateKey("Address1"))
            importedAddressList.add(importedAddressWithPrivateKey("Address1"))
        }.allNonArchivedAccountsAndAddresses() `should equal` listOf("Address1")
    }

    @Test
    fun `one xpub`() {
        Wallet().apply {
            walletBody = walletBody("XPub1")
        }.allNonArchivedAccountsAndAddresses() `should equal` listOf("XPub1")
    }

    @Test
    fun `two xpubs`() {
        Wallet().apply {
            walletBody = walletBody("XPub1", "XPub2")
        }.allNonArchivedAccountsAndAddresses() `should equal` listOf("XPub1", "XPub2")
    }

    @Test
    fun `repeated xpubs`() {
        Wallet().apply {
            walletBody = walletBody("XPub1", "XPub1")
        }.allNonArchivedAccountsAndAddresses() `should equal` listOf("XPub1")
    }

    @Test
    fun `two xpubs, two spendable address and two non-spendable`() {
        Wallet().apply {
            walletBody = walletBody("XPub1", "XPub2")
            importedAddressList.add(importedAddressWithPrivateKey("Address1"))
            importedAddressList.add(importedAddressWithPrivateKey("Address2"))
            importedAddressList.add(
                ImportedAddress().also { it.address = "Address3" })
            importedAddressList.add(importedAddressWithPrivateKey("Address4").apply { archive() })
        }.allNonArchivedAccountsAndAddresses() `should equal` listOf(
            "XPub1",
            "XPub2",
            "Address1",
            "Address2",
            "Address3"
        )
    }

    private fun walletBody(vararg xpubs: String) =
        WalletBody().apply {
            accounts = xpubs.map {
                AccountV3(
                    legacyXpub = it
                )
            }
        }
}
