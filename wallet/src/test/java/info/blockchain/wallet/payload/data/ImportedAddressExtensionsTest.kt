package info.blockchain.wallet.payload.data

import org.amshove.kluent.`should be`
import org.junit.Test

class ImportedAddressExtensionsTest {

    @Test
    fun `is not archived initially`() {
        ImportedAddress().isArchived `should be` false
    }

    @Test
    fun `tag is not set initially`() {
        ImportedAddress().tag `should be` 0
    }

    @Test
    fun `archive marks address as archived`() {
        ImportedAddress().apply {
            archive()
            isArchived `should be` true
        }
    }

    @Test
    fun `tag is set by archive`() {
        ImportedAddress().apply {
            archive()
            tag `should be` ImportedAddress.ARCHIVED_ADDRESS
        }
    }

    @Test
    fun `tag set marks address as archived`() {
        ImportedAddress().apply {
            tag = ImportedAddress.ARCHIVED_ADDRESS
            isArchived `should be` true
        }
    }

    @Test
    fun `tag set to normal, clears archived`() {
        ImportedAddress().apply {
            tag = ImportedAddress.NORMAL_ADDRESS
            isArchived `should be` false
        }
    }

    @Test
    fun `unarchive, clears archived`() {
        ImportedAddress().apply {
            archive()
            unarchive()
            isArchived `should be` false
        }
    }
}