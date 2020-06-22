package piuk.blockchain.android.ui.receive

import com.blockchain.logging.CrashLogger
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.testutils.bitcoin
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.AddressBook
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.payload.data.archive
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.bitcoinj.params.BitcoinCashMainNetParams
import org.bitcoinj.params.BitcoinMainNetParams
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.android.R
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import java.math.BigInteger
import java.util.Locale

class WalletAccountHelperTest {

    private lateinit var subject: WalletAccountHelper
    private val payloadManager: PayloadManager = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val stringUtils: StringUtils = mock()
    private val ethDataManager: EthDataManager = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val bchDataManager: BchDataManager = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val xlmDataManager: XlmDataManager = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val environmentSettings: EnvironmentConfig = mock()
    private val paxAccount: Erc20Account = mock()
    private val crashLogger: CrashLogger = mock()

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)

        subject = WalletAccountHelper(
            payloadManager,
            stringUtils,
            ethDataManager,
            paxAccount,
            bchDataManager,
            xlmDataManager,
            environmentSettings,
            crashLogger
        )

        whenever(environmentSettings.bitcoinCashNetworkParameters)
            .thenReturn(BitcoinCashMainNetParams.get())
        whenever(environmentSettings.bitcoinNetworkParameters)
            .thenReturn(BitcoinMainNetParams.get())

        whenever(stringUtils.getString(R.string.watch_only)).thenReturn("watch only")
        whenever(stringUtils.getString(R.string.address_book_label)).thenReturn("address book")
    }

    @Test
    fun `getLegacyAddresses should return single LegacyAddress`() {
        // Arrange
        val address = "ADDRESS"
        val archivedAddress = LegacyAddress().apply { archive() }
        val legacyAddress = LegacyAddress().apply {
            this.label = null
            this.address = address
        }
        whenever(payloadManager.payload?.legacyAddressList)
            .thenReturn(mutableListOf(archivedAddress, legacyAddress))

        whenever(payloadManager.getAddressBalance(address)).thenReturn(BigInteger.TEN)

        // Act
        val result = subject.getLegacyBtcAddresses()

        // Assert
        verify(payloadManager, atLeastOnce()).payload
        result.size `should equal` 1
        result[0].accountObject `should be` legacyAddress
        result[0].balance `should equal` 0.0000001.bitcoin()
    }

    @Test
    fun `getAddressBookEntries should return single item`() {
        // Arrange
        val addressBook = AddressBook()
        whenever(payloadManager.payload?.addressBook).thenReturn(listOf(addressBook))
        // Act
        val result = subject.getAddressBookEntries()
        // Assert
        result.size `should equal` 1
    }

    @Test
    fun `getAddressBookEntries should return empty list`() {
        // Arrange
        whenever(payloadManager.payload?.addressBook)
            .thenReturn(null)
        // Act
        val result = subject.getAddressBookEntries()
        // Assert
        result.size `should equal` 0
    }
}
