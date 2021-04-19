package info.blockchain.wallet.payload.data

import org.amshove.kluent.`should equal`
import org.junit.Test

class WalletsNonArchivedImportedAddressesExtensionTest {

    private fun importedAddressWithPrivateKey(address: String, privateKey: String = "PRIVATE_KEY") =
        ImportedAddress().also {
            it.privateKey = privateKey
            it.address = address
        }

    @Test
    fun `empty list`() {
        Wallet().nonArchivedImportedAddressStrings() `should equal` emptyList()
    }

    @Test
    fun `one spendable`() {
        Wallet().apply {
            importedAddressList.add(importedAddressWithPrivateKey("Address1"))
        }.nonArchivedImportedAddressStrings() `should equal` listOf("Address1")
    }

    @Test
    fun `one archived`() {
        Wallet().apply {
            importedAddressList.add(importedAddressWithPrivateKey("Address1").apply { archive() })
        }.nonArchivedImportedAddressStrings() `should equal` emptyList()
    }

    @Test
    fun `one without private key`() {
        Wallet().apply {
            importedAddressList.add(ImportedAddress().apply {
                address = "Address1"
            })
        }.nonArchivedImportedAddressStrings() `should equal` listOf("Address1")
    }

    @Test
    fun `two spendable`() {
        Wallet().apply {
            importedAddressList.add(importedAddressWithPrivateKey("Address1", "PRIVATE_KEY1"))
            importedAddressList.add(importedAddressWithPrivateKey("Address2", "PRIVATE_KEY2"))
        }.nonArchivedImportedAddressStrings() `should equal` listOf("Address1", "Address2")
    }

    @Test
    fun `repeated address`() {
        Wallet().apply {
            importedAddressList.add(importedAddressWithPrivateKey("Address1", "PRIVATE_KEY1"))
            importedAddressList.add(importedAddressWithPrivateKey("Address1", "PRIVATE_KEY2"))
        }.nonArchivedImportedAddressStrings() `should equal` listOf("Address1")
    }
}