package piuk.blockchain.androidcore.data.payload

import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.api.data.Balance
import info.blockchain.wallet.exceptions.ApiException
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.ImportedAddress
import info.blockchain.wallet.payload.data.Wallet
import org.amshove.kluent.mock
import org.bitcoinj.core.ECKey
import org.bitcoinj.params.BitcoinMainNetParams
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import com.blockchain.android.testutils.rxInit
import java.util.LinkedHashMap

@Suppress("IllegalIdentifier")
class PayloadServiceTest {

    private lateinit var subject: PayloadService
    private val mockPayloadManager: PayloadManager = mock(defaultAnswer = RETURNS_DEEP_STUBS)
    private val networkParameters = BitcoinMainNetParams.get()

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = PayloadService(mockPayloadManager)
    }

    @Test
    fun initializeFromPayload() {
        // Arrange
        val payload = "PAYLOAD"
        val password = "PASSWORD"
        // Act
        val testObserver =
            subject.initializeFromPayload(networkParameters, payload, password).test()
        // Assert
        verify(mockPayloadManager).initializeAndDecryptFromPayload(
            networkParameters,
            payload,
            password
        )
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
    }

    @Test
    fun restoreHdWallet() {
        // Arrange
        val mnemonic = "MNEMONIC"
        val walletName = "WALLET_NAME"
        val email = "EMAIL"
        val password = "PASSWORD"
        val mockWallet: Wallet = mock()
        whenever(mockPayloadManager.recoverFromMnemonic(mnemonic, walletName, email, password))
            .thenReturn(mockWallet)
        // Act
        val testObserver = subject.restoreHdWallet(mnemonic, walletName, email, password).test()
        // Assert
        verify(mockPayloadManager).recoverFromMnemonic(mnemonic, walletName, email, password)
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
        testObserver.assertValue(mockWallet)
    }

    @Test
    fun createHdWallet() {
        // Arrange
        val password = "PASSWORD"
        val walletName = "WALLET_NAME"
        val email = "EMAIL"
        val mockWallet: Wallet = mock()
        whenever(mockPayloadManager.create(walletName, email, password)).thenReturn(mockWallet)
        // Act
        val testObserver = subject.createHdWallet(password, walletName, email).test()
        // Assert
        verify(mockPayloadManager).create(walletName, email, password)
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
        testObserver.assertValue(mockWallet)
    }

    @Test
    fun initializeAndDecrypt() {
        // Arrange
        val sharedKey = "SHARED_KEY"
        val guid = "GUID"
        val password = "PASSWORD"
        // Act
        val testObserver =
            subject.initializeAndDecrypt(networkParameters, sharedKey, guid, password).test()
        // Assert
        verify(mockPayloadManager).initializeAndDecrypt(
            networkParameters,
            sharedKey,
            guid,
            password
        )
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
    }

    @Test
    fun handleQrCode() {
        // Arrange
        val qrString = "QR_STRING"
        // Act
        val testObserver = subject.handleQrCode(networkParameters, qrString).test()
        // Assert
        verify(mockPayloadManager).initializeAndDecryptFromQR(networkParameters, qrString)
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
    }

    @Test
    fun `upgradeV2toV3 successful`() {
        // Arrange
        val secondPassword = "SECOND_PASSWORD"
        val defaultAccountName = "DEFAULT_ACCOUNT_NAME"
        whenever(mockPayloadManager.upgradeV2PayloadToV3(secondPassword, defaultAccountName))
            .thenReturn(true)
        // Act
        val testObserver = subject.upgradeV2toV3(secondPassword, defaultAccountName).test()
        // Assert
        verify(mockPayloadManager).upgradeV2PayloadToV3(secondPassword, defaultAccountName)
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
    }

    @Test
    fun `upgradeV2toV3 failed`() {
        // Arrange
        val secondPassword = "SECOND_PASSWORD"
        val defaultAccountName = "DEFAULT_ACCOUNT_NAME"
        whenever(mockPayloadManager.upgradeV2PayloadToV3(secondPassword, defaultAccountName))
            .thenReturn(false)
        // Act
        val testObserver = subject.upgradeV2toV3(secondPassword, defaultAccountName).test()
        // Assert
        verify(mockPayloadManager).upgradeV2PayloadToV3(secondPassword, defaultAccountName)
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertNotComplete()
        testObserver.assertError(Throwable::class.java)
    }

    @Test
    fun `syncPayloadWithServer successful`() {
        // Arrange
        whenever(mockPayloadManager.save()).thenReturn(true)
        // Act
        val testObserver = subject.syncPayloadWithServer().test()
        // Assert
        verify(mockPayloadManager).save()
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
    }

    @Test
    fun `syncPayloadWithServer failed`() {
        // Arrange
        whenever(mockPayloadManager.save()).thenReturn(false)
        // Act
        val testObserver = subject.syncPayloadWithServer().test()
        // Assert
        verify(mockPayloadManager).save()
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertNotComplete()
        testObserver.assertError(ApiException::class.java)
    }

    @Test
    fun `syncPayloadAndPublicKeys successful`() {
        // Arrange
        whenever(mockPayloadManager.saveAndSyncPubKeys()).thenReturn(true)
        // Act
        val testObserver = subject.syncPayloadAndPublicKeys().test()
        // Assert
        verify(mockPayloadManager).saveAndSyncPubKeys()
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
    }

    @Test
    fun `syncPayloadAndPublicKeys failed`() {
        // Arrange
        whenever(mockPayloadManager.saveAndSyncPubKeys()).thenReturn(false)
        // Act
        val testObserver = subject.syncPayloadAndPublicKeys().test()
        // Assert
        verify(mockPayloadManager).saveAndSyncPubKeys()
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertNotComplete()
        testObserver.assertError(ApiException::class.java)
    }

    @Test
    fun updateAllTransactions() {
        // Arrange

        // Act
        val testObserver = subject.updateAllTransactions().test()
        // Assert
        verify(mockPayloadManager).getAllTransactions(50, 0)
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
    }

    @Test
    fun updateAllBalances() {
        // Arrange

        // Act
        val testObserver = subject.updateAllBalances().test()
        // Assert
        verify(mockPayloadManager).updateAllBalances()
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
    }

    @Test
    fun getBalanceOfAddresses() {
        // Arrange
        val addresses = listOf("address_one", "address_two", "address_three")
        val map = mapOf(
            Pair(
                "address_one", Balance()
            ),
            Pair("address_two", Balance()),
            Pair("address_three", Balance())
        )
        val linkedMap = LinkedHashMap(map)
        whenever(mockPayloadManager.getBalanceOfBtcAddresses(addresses))
            .thenReturn(linkedMap)
        // Act
        val testObserver = subject.getBalanceOfBtcAddresses(addresses).test()
        // Assert
        verify(mockPayloadManager).getBalanceOfBtcAddresses(addresses)
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
        testObserver.assertValue(linkedMap)
    }

    @Test
    fun getBalanceOfBchAddresses() {
        // Arrange
        val addresses = listOf("address_one", "address_two", "address_three")
        val map = mapOf(
            Pair(
                "address_one", Balance()
            ),
            Pair("address_two", Balance()),
            Pair("address_three", Balance())
        )
        val linkedMap = LinkedHashMap(map)
        whenever(mockPayloadManager.getBalanceOfBchAddresses(addresses))
            .thenReturn(linkedMap)
        // Act
        val testObserver = subject.getBalanceOfBchAddresses(addresses).test()
        // Assert
        verify(mockPayloadManager).getBalanceOfBchAddresses(addresses)
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
        testObserver.assertValue(linkedMap)
    }

    @Test
    fun updateTransactionNotes() {
        // Arrange
        val txHash = "TX_HASH"
        val note = "NOTE"
        whenever(mockPayloadManager.payload?.txNotes).thenReturn(mutableMapOf())
        whenever(mockPayloadManager.save()).thenReturn(true)
        // Act
        val testObserver = subject.updateTransactionNotes(txHash, note).test()
        // Assert
        verify(mockPayloadManager, atLeastOnce()).payload
        verify(mockPayloadManager).save()
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
    }

    @Test
    fun createNewAccount() {
        // Arrange
        val label = "LABEL"
        val secondPassword = "SECOND_PASSWORD"
        val mockAccount: Account = mock()
        whenever(
            mockPayloadManager.addAccount(
                networkParameters,
                label,
                secondPassword
            )
        ).thenReturn(mockAccount)
        // Act
        val testObserver = subject.createNewAccount(networkParameters, label, secondPassword).test()
        // Assert
        verify(mockPayloadManager).addAccount(networkParameters, label, secondPassword)
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
        testObserver.assertValue(mockAccount)
    }

    @Test
    fun setKeyForImportedAddress() {
        // Arrange
        val mockEcKey: ECKey = mock()
        val secondPassword = "SECOND_PASSWORD"
        val mockImportedAddress: ImportedAddress = mock()
        whenever(mockPayloadManager.setKeyForImportedAddress(mockEcKey, secondPassword))
            .thenReturn(mockImportedAddress)
        // Act
        val testObserver = subject.setKeyForImportedAddress(mockEcKey, secondPassword).test()
        // Assert
        verify(mockPayloadManager).setKeyForImportedAddress(mockEcKey, secondPassword)
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
        testObserver.assertValue(mockImportedAddress)
    }

    @Test
    fun addImportedAddress() {
        // Arrange
        val mockImportedAddress: ImportedAddress = mock()
        // Act
        val testObserver = subject.addImportedAddress(mockImportedAddress).test()
        // Assert
        verify(mockPayloadManager).addImportedAddress(mockImportedAddress)
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
    }

    @Test
    fun updateImportedAddress() {
        // Arrange
        val mockImportedAddress: ImportedAddress = mock()
        // Act
        val testObserver = subject.updateImportedAddress(mockImportedAddress).test()
        // Assert
        verify(mockPayloadManager).updateImportedAddress(mockImportedAddress)
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
    }
}