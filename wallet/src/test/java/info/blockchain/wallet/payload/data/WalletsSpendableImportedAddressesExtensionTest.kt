package info.blockchain.wallet.payload.data

import org.amshove.kluent.`should equal`
import org.junit.Test

class WalletsSpendableImportedAddressesExtensionTest {

    private fun importedAddressWithPrivateKey(address: String, privateKey: String = "PRIVATE_KEY") =
        ImportedAddress().also {
            it.privateKey = privateKey
            it.address = address
        }

    @Test
    fun `empty list`() {
        Wallet().spendableImportedAddressStrings() `should equal` emptySet()
    }

    @Test
    fun `one spendable`() {
        Wallet().apply {
            importedAddressList.add(importedAddressWithPrivateKey("Address1"))
        }.spendableImportedAddressStrings() `should equal` setOf("Address1")
    }

    @Test
    fun `one archived`() {
        Wallet().apply {
            importedAddressList.add(importedAddressWithPrivateKey("Address1").apply { archive() })
        }.spendableImportedAddressStrings() `should equal` emptySet()
    }

    @Test
    fun `two spendable`() {
        Wallet().apply {
            importedAddressList.add(importedAddressWithPrivateKey("Address1", "PRIVATE_KEY1"))
            importedAddressList.add(importedAddressWithPrivateKey("Address2", "PRIVATE_KEY2"))
        }.spendableImportedAddressStrings() `should equal` setOf("Address1", "Address2")
    }

    @Test
    fun `repeated address`() {
        Wallet().apply {
            importedAddressList.add(importedAddressWithPrivateKey("Address1", "PRIVATE_KEY1"))
            importedAddressList.add(importedAddressWithPrivateKey("Address1", "PRIVATE_KEY2"))
        }.spendableImportedAddressStrings() `should equal` setOf("Address1")
    }
}