package piuk.blockchain.androidcore.data.payload

import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.ImportedAddress
import info.blockchain.wallet.payload.data.Wallet
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import com.blockchain.android.testutils.rxInit
import info.blockchain.api.ApiException
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.model.Balance
import io.reactivex.Single
import org.amshove.kluent.itReturns

@Suppress("IllegalIdentifier")
class PayloadServiceTest {

    private lateinit var subject: PayloadService
    private val mockPayloadManager: PayloadManager = mock(defaultAnswer = RETURNS_DEEP_STUBS)
    private val versionController: PayloadVersionController = mock {
        on { isFullRolloutV4 } itReturns false
    }

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = PayloadService(mockPayloadManager, versionController)
    }

    @Test
    fun initializeFromPayload() {
        // Arrange
        val payload = "PAYLOAD"
        val password = "PASSWORD"
        // Act
        val testObserver =
            subject.initializeFromPayload(payload, password).test()
        // Assert
        verify(mockPayloadManager).initializeAndDecryptFromPayload(
            payload,
            password
        )
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
    }

    @Test
    fun restoreHdWallet_v3() {
        // Arrange
        val mnemonic = "MNEMONIC"
        val walletName = "WALLET_NAME"
        val email = "EMAIL"
        val password = "PASSWORD"
        val mockWallet: Wallet = mock()
        val v4Enabled = false
        whenever(mockPayloadManager.recoverFromMnemonic(mnemonic, walletName, email, password, v4Enabled))
            .thenReturn(mockWallet)

        // Act
        val testObserver = subject.restoreHdWallet(mnemonic, walletName, email, password).test()

        // Assert
        verify(mockPayloadManager).recoverFromMnemonic(mnemonic, walletName, email, password, v4Enabled)
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
        testObserver.assertValue(mockWallet)
    }

    @Test
    fun restoreHdWallet_v4() {
        // Arrange
        val mnemonic = "MNEMONIC"
        val walletName = "WALLET_NAME"
        val email = "EMAIL"
        val password = "PASSWORD"
        val mockWallet: Wallet = mock()
        val v4Enabled = true
        whenever(versionController.isFullRolloutV4).thenReturn(true)
        whenever(mockPayloadManager.recoverFromMnemonic(mnemonic, walletName, email, password, v4Enabled))
            .thenReturn(mockWallet)

        // Act
        val testObserver = subject.restoreHdWallet(mnemonic, walletName, email, password).test()

        // Assert
        verify(mockPayloadManager).recoverFromMnemonic(mnemonic, walletName, email, password, v4Enabled)
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
        testObserver.assertValue(mockWallet)
    }

    @Test
    fun createHdWallet_v3() {
        // Arrange
        val password = "PASSWORD"
        val walletName = "WALLET_NAME"
        val email = "EMAIL"
        val mockWallet: Wallet = mock()
        val v4Enabled = false

        whenever(mockPayloadManager.create(walletName, email, password, v4Enabled))
            .thenReturn(mockWallet)

        // Act
        val testObserver = subject.createHdWallet(password, walletName, email).test()

        // Assert
        verify(mockPayloadManager).create(walletName, email, password, v4Enabled)
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
        testObserver.assertValue(mockWallet)
    }

    @Test
    fun createHdWallet_v4() {
        // Arrange
        val password = "PASSWORD"
        val walletName = "WALLET_NAME"
        val email = "EMAIL"
        val mockWallet: Wallet = mock()
        val v4Enabled = true
        whenever(versionController.isFullRolloutV4).thenReturn(true)
        whenever(mockPayloadManager.create(walletName, email, password, v4Enabled)).thenReturn(mockWallet)
        // Act
        val testObserver = subject.createHdWallet(password, walletName, email).test()
        // Assert
        verify(mockPayloadManager).create(walletName, email, password, v4Enabled)
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
        testObserver.assertValue(mockWallet)
    }

    @Test
    fun initializeAndDecrypt_v3() {
        // Arrange
        val sharedKey = "SHARED_KEY"
        val guid = "GUID"
        val password = "PASSWORD"
        val v4Enabled = false
        whenever(versionController.isV4Enabled(guid, sharedKey))
            .thenReturn(Single.just(v4Enabled))

        // Act
        val testObserver = subject.initializeAndDecrypt(sharedKey, guid, password)
            .test()

        // Assert
        verify(mockPayloadManager).initializeAndDecrypt(
            sharedKey,
            guid,
            password,
            v4Enabled
        )
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
    }

    @Test
    fun initializeAndDecrypt_v4() {
        // Arrange
        val sharedKey = "SHARED_KEY"
        val guid = "GUID"
        val password = "PASSWORD"
        val v4Enabled = true
        whenever(versionController.isV4Enabled(guid, sharedKey))
            .thenReturn(Single.just(v4Enabled))

        // Act
        val testObserver = subject.initializeAndDecrypt(sharedKey, guid, password)
            .test()

        // Assert
        verify(mockPayloadManager).initializeAndDecrypt(
            sharedKey,
            guid,
            password,
            v4Enabled
        )
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
    }

    @Test
    fun handleQrCode_v3() {
        // Arrange
        val qrString = "QR_STRING"
        val v4Enabled = false

        // Act
        val testObserver = subject.handleQrCode(qrString).test()
        // Assert
        verify(mockPayloadManager).initializeAndDecryptFromQR(qrString, v4Enabled)
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
    }

    @Test
    fun handleQrCode_v4() {
        // Arrange
        val qrString = "QR_STRING"
        val v4Enabled = true
        whenever(versionController.isFullRolloutV4).thenReturn(true)

        // Act
        val testObserver = subject.handleQrCode(qrString).test()

        // Assert
        verify(mockPayloadManager).initializeAndDecryptFromQR(qrString, v4Enabled)
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
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
    fun getBalanceOfBchAddresses() {
        // Arrange
        val addresses = listOf("address_one", "address_two", "address_three")
        val map = mapOf(
            Pair("address_one", mock(Balance::class)),
            Pair("address_two", mock(Balance::class)),
            Pair("address_three", mock(Balance::class))
        )

        val xpubs = addresses.map { XPubs(XPub(it, XPub.Format.LEGACY)) }
        whenever(mockPayloadManager.getBalanceOfBchAccounts(xpubs))
            .thenReturn(map)
        // Act
        val testObserver = subject.getBalanceOfBchAccounts(xpubs).test()
        // Assert
        verify(mockPayloadManager).getBalanceOfBchAccounts(xpubs)
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
        testObserver.assertValue(map)
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
                label,
                secondPassword
            )
        ).thenReturn(mockAccount)
        // Act
        val testObserver = subject.createNewAccount(label, secondPassword).test()
        // Assert
        verify(mockPayloadManager).addAccount(label, secondPassword)
        verifyNoMoreInteractions(mockPayloadManager)
        testObserver.assertComplete()
        testObserver.assertValue(mockAccount)
    }

    @Test
    fun setKeyForImportedAddress() {
        // Arrange
        val mockKey: SigningKey = mock()
        val secondPassword = "SECOND_PASSWORD"
        val mockImportedAddress: ImportedAddress = mock()
        whenever(mockPayloadManager.setKeyForImportedAddress(mockKey, secondPassword))
            .thenReturn(mockImportedAddress)
        // Act
        val testObserver = subject.setKeyForImportedAddress(mockKey, secondPassword).test()
        // Assert
        verify(mockPayloadManager).setKeyForImportedAddress(mockKey, secondPassword)
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