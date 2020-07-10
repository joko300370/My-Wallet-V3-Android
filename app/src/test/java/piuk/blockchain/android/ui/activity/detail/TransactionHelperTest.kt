package piuk.blockchain.android.ui.activity.detail

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import info.blockchain.wallet.payload.data.Wallet
import org.junit.Test
import piuk.blockchain.android.coincore.TestNonCustodialSummaryItem
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import kotlin.test.assertEquals

@Suppress("PrivatePropertyName")
class TransactionHelperTest {
    private val payloadDataManager: PayloadDataManager = mock()
    private val bchDataManager: BchDataManager = mock()
    private val payload: Wallet = mock()

    private val cryptoValBtc_1 = CryptoValue.bitcoinFromSatoshis(1)
    private val cryptoValBtc_15 = CryptoValue.bitcoinFromSatoshis(15)
    private val cryptoValBch_1 = CryptoValue.bitcoinCashFromSatoshis(1)
    private val cryptoValBch_15 = CryptoValue.bitcoinCashFromSatoshis(15)

    private val subject: TransactionHelper =
        TransactionHelper(
            payloadDataManager,
            bchDataManager
        )

    @Test
    fun filterNonChangeAddressesSingleInput() {
        // Arrange
        val item = TestNonCustodialSummaryItem(
            exchangeRates = mock(),
            direction = TransactionSummary.Direction.RECEIVED,
            inputsMap = mapOf(
                "key" to cryptoValBtc_1
            )
        )

        // Act
        val value = subject.filterNonChangeBtcAddresses(item)

        // Assert
        assertEquals(1, value.left.size)
        assertEquals(0, value.right.size)
    }

    @Test
    fun filterNonChangeReceivedAddressesMultipleInput() { // Arrange
        val item = TestNonCustodialSummaryItem(
            direction = TransactionSummary.Direction.RECEIVED,
            inputsMap = mapOf(
                "key0" to cryptoValBtc_1,
                "key1" to cryptoValBtc_1
            )
        )

        // Act
        val value = subject.filterNonChangeBtcAddresses(item)

        // Assert
        assertEquals(1, value.left.size)
        assertEquals(0, value.right.size)
    }

    @Test
    fun filterNonChangeAddressesMultipleInput() {
        // Arrange
        val item = TestNonCustodialSummaryItem(
            direction = TransactionSummary.Direction.SENT,
            inputsMap = mapOf(
                "key0" to cryptoValBtc_1,
                "key1" to cryptoValBtc_1,
                "key2" to cryptoValBtc_1
            )
        )

        whenever(payloadDataManager.getXpubFromAddress("key0"))
            .thenReturn("xpub")
        whenever(payloadDataManager.getXpubFromAddress("key1"))
            .thenReturn("xpub")

        // Act
        val value = subject.filterNonChangeBtcAddresses(item)

        // Assert
        assertEquals(2, value.left.size)
        assertEquals(0, value.right.size)
    }

    @Test
    fun filterNonChangeAddressesSingleInputSingleOutput() {
        // Arrange
        val item = TestNonCustodialSummaryItem(
            direction = TransactionSummary.Direction.SENT,
            inputsMap = mapOf(
                "key" to cryptoValBtc_1
            ),
            outputsMap = mapOf(
                "key" to cryptoValBtc_1
            )
        )

        whenever(payload.legacyAddressStringList)
            .thenReturn(emptyList())
        whenever(payloadDataManager.wallet)
            .thenReturn(payload)

        // Act
        val value = subject.filterNonChangeBtcAddresses(item)

        // Assert
        assertEquals(1, value.left.size)
        assertEquals(1, value.right.size)
    }

    @Test
    fun filterNonChangeAddressesSingleInputMultipleOutput() {
        // Arrange
        val item = TestNonCustodialSummaryItem(
            direction = TransactionSummary.Direction.SENT,
            inputsMap = mapOf(
                "key0" to cryptoValBtc_1
            ),
            outputsMap = mapOf(
                "key0" to cryptoValBtc_1,
                "key1" to cryptoValBtc_1,
                "key2" to cryptoValBtc_15
            ),
            value = CryptoValue.bitcoinFromSatoshis(10)
        )

        val legacyStrings = listOf("key0", "key1")
        val watchOnlyStrings = listOf("key2")

        whenever(payload.legacyAddressStringList)
            .thenReturn(legacyStrings)
        whenever(payload.watchOnlyAddressStringList)
            .thenReturn(watchOnlyStrings)

        whenever(payloadDataManager.wallet)
            .thenReturn(payload)

        // Act
        val value = subject.filterNonChangeBtcAddresses(item)

        // Assert
        assertEquals(1, value.left.size)
        assertEquals(1, value.right.size)
    }

    @Test
    fun filterNonChangeAddressesSingleInputSingleOutputHD() {
        // Arrange
        val item = TestNonCustodialSummaryItem(
            direction = TransactionSummary.Direction.SENT,
            inputsMap = mapOf(
                "key0" to cryptoValBtc_1
            ),
            outputsMap = mapOf(
                "key0" to cryptoValBtc_1
            ),
            value = CryptoValue.bitcoinFromSatoshis(10)
        )

        val legacyStrings = listOf("key0", "key1")
        val watchOnlyStrings = listOf("key2")

        whenever(payload.legacyAddressStringList)
            .thenReturn(legacyStrings)
        whenever(payload.watchOnlyAddressStringList)
            .thenReturn(watchOnlyStrings)
        whenever(payloadDataManager.wallet)
            .thenReturn(payload)
        whenever(payloadDataManager.isOwnHDAddress(any()))
            .thenReturn(true)

        // Act
        val value = subject.filterNonChangeBtcAddresses(item)

        // Assert
        assertEquals(1, value.left.size)
        assertEquals(1, value.right.size)
    }

    @Test
    fun filterNonChangeAddressesMultipleInputBch() {
        // Arrange
        val item = TestNonCustodialSummaryItem(
            cryptoCurrency = CryptoCurrency.BCH,
            direction = TransactionSummary.Direction.SENT,
            inputsMap = mapOf(
                "key0" to cryptoValBch_1,
                "key1" to cryptoValBch_1,
                "key2" to cryptoValBch_1
            )
        )

        whenever(bchDataManager.getXpubFromAddress("key0"))
            .thenReturn("xpub")
        whenever(bchDataManager.getXpubFromAddress("key1"))
            .thenReturn("xpub")

        // Act
        val value = subject.filterNonChangeBchAddresses(item)

        // Assert
        assertEquals(2, value.left.size)
        assertEquals(0, value.right.size)
    }

    @Test
    fun filterNonChangeAddressesSingleInputSingleOutputBch() {
        // Arrange
        val item = TestNonCustodialSummaryItem(
            cryptoCurrency = CryptoCurrency.BCH,
            direction = TransactionSummary.Direction.SENT,
            inputsMap = mapOf(
                "key" to cryptoValBch_1
            ),
            outputsMap = mapOf(
                "key" to cryptoValBch_1
            )
        )

        whenever(bchDataManager.getLegacyAddressStringList())
            .thenReturn(emptyList())

        // Act
        val value = subject.filterNonChangeBchAddresses(item)

        // Assert
        assertEquals(1, value.left.size)
        assertEquals(1, value.right.size)
    }

    @Test
    fun filterNonChangeAddressesSingleInputMultipleOutputBch() {
        // Arrange
        val item = TestNonCustodialSummaryItem(
            direction = TransactionSummary.Direction.SENT,
            inputsMap = mapOf(
                "key0" to cryptoValBch_1
            ),
            outputsMap = mapOf(
                "key0" to cryptoValBch_1,
                "key1" to cryptoValBch_1,
                "key2" to cryptoValBch_15
            ),
            value = CryptoValue.bitcoinCashFromSatoshis(10)
        )

        val legacyStrings = listOf("key0", "key1")
        val watchOnlyStrings = listOf("key2")

        whenever(bchDataManager.getLegacyAddressStringList())
            .thenReturn(legacyStrings)
        whenever(bchDataManager.getWatchOnlyAddressStringList())
            .thenReturn(watchOnlyStrings)

        // Act
        val value = subject.filterNonChangeBchAddresses(item)

        // Assert
        assertEquals(1, value.left.size)
        assertEquals(1, value.right.size)
    }

    @Test
    fun filterNonChangeAddressesSingleInputSingleOutputHDBch() {
        // Arrange
        val item = TestNonCustodialSummaryItem(
            direction = TransactionSummary.Direction.SENT,
            inputsMap = mapOf(
                "key0" to cryptoValBch_1
            ),
            outputsMap = mapOf(
                "key0" to cryptoValBch_1
            ),
            value = CryptoValue.bitcoinFromSatoshis(10)
        )

        val legacyStrings = listOf("key0", "key1")
        val watchOnlyStrings = listOf("key2")

        whenever(bchDataManager.getLegacyAddressStringList())
            .thenReturn(legacyStrings)
        whenever(bchDataManager.getWatchOnlyAddressStringList())
            .thenReturn(watchOnlyStrings)
        whenever(bchDataManager.isOwnAddress(any()))
            .thenReturn(true)

        // Act
        val value = subject.filterNonChangeBchAddresses(item)

        // Assert
        assertEquals(1, value.right.size)
        assertEquals(1, value.left.size)
    }
}
