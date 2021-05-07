package piuk.blockchain.android.ui.auth

import android.content.Intent
import android.view.View
import android.widget.ImageView
import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.datamanagers.ApiStatus
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.remoteconfig.RemoteConfig
import com.blockchain.wallet.DefaultLabels
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.isNull
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.data.UpdateType
import info.blockchain.wallet.exceptions.AccountLockedException
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import info.blockchain.wallet.exceptions.ServerConnectionException
import info.blockchain.wallet.exceptions.UnsupportedVersionException
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.Wallet
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.itReturns
import org.amshove.kluent.mock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.spongycastle.crypto.InvalidCipherTextException
import piuk.blockchain.android.BlockchainTestApplication
import piuk.blockchain.android.data.biometrics.BiometricsController
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.home.CredentialsWiper
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.PrngFixer
import java.net.SocketTimeoutException

@Config(sdk = [23], application = BlockchainTestApplication::class) @RunWith(
    RobolectricTestRunner::class
)
class PinEntryPresenterTest {

    private val mockImageView = Mockito.mock(ImageView::class.java)
    private val activity: PinEntryView = mock {
        on { pinBoxList } itReturns listOf(mockImageView, mockImageView, mockImageView, mockImageView)
    }

    private val authDataManager: AuthDataManager = mock()
    private val appUtil: AppUtil = mock()
    private val prefsUtil: PersistentPrefs = mock {
        on { walletGuid } itReturns WALLET_GUID
        on { sharedKey } itReturns SHARED_KEY
    }

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private val payloadManager: PayloadDataManager = mock()
    private val defaultLabels: DefaultLabels = mock {
        on { getDefaultNonCustodialWalletLabel(any()) } itReturns "string resource"
    }

    private val biometricsController: BiometricsController = mock()
    private val accessState: AccessState = mock()
    private val walletOptionsDataManager: WalletOptionsDataManager = mock()
    private val prngFixer: PrngFixer = mock()
    private val mobileNoticeRemoteConfig: MobileNoticeRemoteConfig = mock()
    private val crashLogger: CrashLogger = mock()
    private val analytics: Analytics = mock()
    private val remoteConfig: RemoteConfig = mock()
    private val credentialsWiper: CredentialsWiper = mock()

    private val apiStatus: ApiStatus = mock {
        on { isHealthy() } itReturns Single.just(true)
    }

    private val subject: PinEntryPresenter = PinEntryPresenter(
        analytics = analytics,
        authDataManager = authDataManager,
        appUtil = appUtil,
        prefs = prefsUtil,
        payloadDataManager = payloadManager,
        defaultLabels = defaultLabels,
        accessState = accessState,
        walletOptionsDataManager = walletOptionsDataManager,
        prngFixer = prngFixer,
        mobileNoticeRemoteConfig = mobileNoticeRemoteConfig,
        crashLogger = crashLogger,
        apiStatus = apiStatus,
        credentialsWiper = credentialsWiper,
        biometricsController = biometricsController
    )

    @Before
    fun init() {
        subject.initView(activity)
    }

    @Test
    fun onViewReadyValidatingPinForResult() {
        // Arrange
        val intent = Intent()
        intent.putExtra(KEY_VALIDATING_PIN_FOR_RESULT, true)
        whenever(activity.pageIntent).thenReturn(intent)
        whenever(remoteConfig.getFeatureCount(any())).thenReturn(Single.just(4L))

        // Act
        subject.onViewReady()

        // Assert
        assertTrue(subject.isForValidatingPinForResult)
    }

    @Test
    fun onViewReadyMaxAttemptsExceeded() {
        // Arrange
        whenever(activity.pageIntent).thenReturn(null)
        whenever(prefsUtil.pinFails).thenReturn(4)
        whenever(payloadManager.wallet).thenReturn(mock(Wallet::class))
        whenever(prefsUtil.pinId).thenReturn("")
        whenever(biometricsController.getDecodedData()).thenReturn("")
        whenever(remoteConfig.getFeatureCount(anyString())).thenReturn(Single.just(4L))

        // Act
        subject.onViewReady()

        // Assert
        assertTrue(subject.allowExit())
        verify(activity).showParameteredToast(anyInt(), anyString(), anyInt())
        verify(activity).showMaxAttemptsDialog()
    }

    @Test
    fun checkFingerprintStatusShouldShowDialog() {
        // Arrange
        subject.isForValidatingPinForResult = false
        whenever(prefsUtil.pinId).thenReturn("1234")
        whenever(biometricsController.isFingerprintUnlockEnabled).thenReturn(true)

        // Act
        subject.checkFingerprintStatus()

        // Assert
        verify(activity).showFingerprintDialog()
    }

    @Test
    fun checkFingerprintStatusDontShow() {
        // Arrange
        subject.isForValidatingPinForResult = true
        // Act
        subject.checkFingerprintStatus()
        // Assert
        verify(activity).showKeyboard()
    }

    @Test
    fun canShowFingerprintDialog() {
        // Arrange
        subject.canShowFingerprintDialog = true
        // Act
        val value = subject.canShowFingerprintDialog()
        // Assert
        assertTrue(value)
    }

    @Test
    fun loginWithDecryptedPin() {
        // Arrange
        val pincode = "1234"
        whenever(authDataManager.validatePin(pincode)).thenReturn(Observable.just("password"))
        // Act
        subject.loginWithDecryptedPin(pincode)
        // Assert
        assertFalse(subject.canShowFingerprintDialog())

        verify(authDataManager).validatePin(pincode)
        verify(activity).pinBoxList
    }

    @Test
    fun onDeleteClicked() {
        // Arrange
        subject.userEnteredPin = "1234"
        // Act
        subject.onDeleteClicked()
        // Assert
        assertEquals("123", subject.userEnteredPin)
        verify(activity).pinBoxList
    }

    @Test
    fun padClickedPinAlreadyFourDigits() {
        // Arrange
        subject.userEnteredPin = "0000"
        // Act
        subject.onPadClicked("0")
        // Assert
        verifyZeroInteractions(activity)
    }

    @Test
    fun padClickedAllZeros() {
        // Arrange
        subject.userEnteredPin = "000"

        whenever(prefsUtil.pinId).thenReturn("")
        whenever(biometricsController.getDecodedData()).thenReturn("")

        // Act
        subject.onPadClicked("0")

        // Assert
        assertEquals("", subject.userEnteredPin)
        assertNull(subject.userEnteredConfirmationPin)

        verify(activity).clearPinBoxes()
        verify(activity).showToast(anyInt(), anyString())
        verify(activity).dismissProgressDialog()
        verify(activity, atLeastOnce()).pinBoxList
        verify(activity).showKeyboard()
        verify(activity).setTitleString(anyInt())

        verifyNoMoreInteractions(activity)
    }

    @Test
    fun padClickedShowCommonPinWarning() {
        // Arrange
        subject.userEnteredPin = "123"
        whenever(prefsUtil.pinId).thenReturn("")

        // Act
        subject.onPadClicked("4")

        // Assert
        verify(activity).showCommonPinWarning(any())
    }

    @Test fun padClickedShowCommonPinWarningAndClickRetry() {
        // Arrange
        subject.userEnteredPin = "123"
        whenever(prefsUtil.pinId).thenReturn("")
        doAnswer { invocation: InvocationOnMock ->
            (invocation.arguments[0] as DialogButtonCallback).onPositiveClicked()
            null
        }.whenever(activity).showCommonPinWarning(any())

        // Act
        subject.onPadClicked("4")

        // Assert
        assertEquals("", subject.userEnteredPin)
        assertNull(subject.userEnteredConfirmationPin)

        verify(activity).showCommonPinWarning(any())
        verify(activity).clearPinBoxes()
    }

    @Test fun padClickedShowCommonPinWarningAndClickContinue() {
        // Arrange
        subject.userEnteredPin = "123"
        whenever(prefsUtil.pinId).thenReturn("")
        doAnswer { invocation: InvocationOnMock ->
            (invocation.arguments[0] as DialogButtonCallback).onNegativeClicked()
            null
        }.whenever(activity).showCommonPinWarning(any())

        // Act
        subject.onPadClicked("4")

        // Assert
        assertEquals("", subject.userEnteredPin)
        assertEquals("1234", subject.userEnteredConfirmationPin)

        verify(activity).showCommonPinWarning(any())
    }

    @Test
    fun padClickedShowPinReuseWarning() {
        // Arrange
        subject.userEnteredPin = "258"
        whenever(prefsUtil.pinId).thenReturn("")
        whenever(accessState.pin).thenReturn("2580")

        // Act
        subject.onPadClicked("0")

        // Assert
        verify(activity).dismissProgressDialog()
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR))
        verify(activity).clearPinBoxes()
    }

    @Test
    fun padClickedVerifyPinValidateCalled() {
        // Arrange
        subject.userEnteredPin = "133"
        whenever(prefsUtil.pinId).thenReturn("1234567890")
        whenever(authDataManager.validatePin(anyString())).thenReturn(Observable.just(""))

        // Act
        subject.onPadClicked("7")

        // Assert
        verify(activity).setTitleVisibility(View.INVISIBLE)
        verify(activity).showProgressDialog(anyInt())
        verify(activity, atLeastOnce()).dismissProgressDialog()
        verify(authDataManager).validatePin(anyString())
    }

    @Test
    fun padClickedVerifyPinForResultReturnsValidPassword() {
        // Arrange
        subject.userEnteredPin = "133"
        subject.isForValidatingPinForResult = true
        whenever(prefsUtil.pinId).thenReturn("1234567890")
        whenever(authDataManager.validatePin(anyString())).thenReturn(Observable.just(""))

        // Act
        subject.onPadClicked("7")

        // Assert
        verify(activity).setTitleVisibility(View.INVISIBLE)
        verify(activity).showProgressDialog(anyInt())
        verify(activity).dismissProgressDialog()
        verify(authDataManager).validatePin(anyString())
        verify(activity).finishWithResultOk("1337")
    }

    @Test
    fun padClickedVerifyPinValidateCalledReturnsErrorIncrementsFailureCount() {
        // Arrange
        subject.userEnteredPin = "133"
        whenever(prefsUtil.pinId).thenReturn("1234567890")
        whenever(authDataManager.validatePin(anyString()))
            .thenReturn(Observable.error(InvalidCredentialsException()))

        // Act
        subject.onPadClicked("7")

        // Assert
        verify(activity).setTitleVisibility(View.INVISIBLE)
        verify(activity).showProgressDialog(anyInt())
        verify(authDataManager).validatePin(anyString())
        verify(prefsUtil).pinFails = anyInt()
        verify(prefsUtil).pinFails
        verify(activity).showToast(anyInt(), anyString())
        verify(activity).restartPageAndClearTop()
    }

    @Test
    fun padClickedVerifyPinValidateCalledReturnsServerError() {
        // Arrange
        subject.userEnteredPin = "133"
        whenever(prefsUtil.pinId).thenReturn("1234567890")
        whenever(authDataManager.validatePin(anyString()))
            .thenReturn(Observable.error(ServerConnectionException()))

        // Act
        subject.onPadClicked("7")

        // Assert
        verify(activity).setTitleVisibility(View.INVISIBLE)
        verify(activity).showProgressDialog(anyInt())
        verify(authDataManager).validatePin(anyString())
        verify(activity).showToast(anyInt(), anyString())
        verify(activity).restartPageAndClearTop()
    }

    @Test
    fun padClickedVerifyPinValidateCalledReturnsTimeout() {
        // Arrange
        subject.userEnteredPin = "133"
        whenever(prefsUtil.pinId).thenReturn("1234567890")
        whenever(authDataManager.validatePin(anyString()))
            .thenReturn(Observable.error(SocketTimeoutException()))

        // Act
        subject.onPadClicked("7")

        // Assert
        verify(activity).setTitleVisibility(View.INVISIBLE)
        verify(activity).showProgressDialog(anyInt())
        verify(authDataManager).validatePin(anyString())
        verify(activity).showToast(anyInt(), anyString())
        verify(activity).restartPageAndClearTop()
    }

    @Test
    fun padClickedVerifyPinValidateCalledReturnsInvalidCipherText() {
        // Arrange
        val password = "password"
        subject.userEnteredPin = "133"
        whenever(prefsUtil.pinId).thenReturn("1234567890")
        whenever(authDataManager.validatePin(anyString())).thenReturn(Observable.just(password))
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(Completable.error(InvalidCipherTextException()))

        // Act
        subject.onPadClicked("7")

        // Assert
        verify(activity).setTitleVisibility(View.INVISIBLE)
        verify(activity, times(2)).showProgressDialog(anyInt())
        verify(activity, atLeastOnce()).dismissProgressDialog()
        verify(authDataManager).validatePin(anyString())
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(activity).showToast(anyInt(), anyString())
        verify(accessState).clearPin()
        verify(appUtil).clearCredentials()
        verify(appUtil).restartApp(LauncherActivity::class.java)
        verify(prefsUtil).sharedKey
        verify(prefsUtil).walletGuid
        verify(prefsUtil, atLeastOnce()).pinId
        verify(prefsUtil).pinFails = anyInt()

        verifyNoMoreInteractions(prefsUtil)
    }

    @Test
    fun padClickedVerifyPinValidateCalledReturnsGenericException() {
        // Arrange
        val password = "password"
        subject.userEnteredPin = "133"
        whenever(prefsUtil.pinId).thenReturn("1234567890")
        whenever(authDataManager.validatePin(anyString())).thenReturn(Observable.just(password))
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(Completable.error(Exception()))

        // Act
        subject.onPadClicked("7")

        // Assert
        verify(activity).setTitleVisibility(View.INVISIBLE)
        verify(activity).showToast(anyInt(), anyString())
        verify(activity, atLeastOnce()).pinBoxList
        verify(activity, times(2)).showProgressDialog(anyInt())
        verify(activity, times(2)).dismissProgressDialog()
        verify(authDataManager).validatePin(anyString())
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(prefsUtil).pinFails = anyInt()
        verify(prefsUtil, atLeastOnce()).pinId
        verify(prefsUtil).walletGuid
        verify(prefsUtil).sharedKey
        verify(appUtil).restartApp(LauncherActivity::class.java)

        verifyNoMoreInteractions(prefsUtil)
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun padClickedCreatePinCreateSuccessful() {
        // Arrange
        subject.userEnteredPin = "133"
        subject.userEnteredConfirmationPin = "1337"
        whenever(payloadManager.tempPassword).thenReturn("temp password")
        whenever(prefsUtil.pinId).thenReturn("")
        whenever(authDataManager.createPin(anyString(), anyString())).thenReturn(Completable.complete())
        whenever(authDataManager.validatePin(anyString())).thenReturn(Observable.just("password"))
        whenever(accessState.pin).thenReturn("1337")

        // Act
        subject.onPadClicked("7")

        // Assert
        verify(activity).showProgressDialog(anyInt())
        verify(activity).dismissProgressDialog()
        verify(authDataManager).createPin(anyString(), anyString())
        verify(biometricsController).setFingerprintUnlockEnabled(false)
    }

    @Test
    fun padClickedCreatePinCreateFailed() {
        // Arrange
        subject.userEnteredPin = "133"
        subject.userEnteredConfirmationPin = "1337"
        whenever(payloadManager.tempPassword).thenReturn("temp password")
        whenever(prefsUtil.pinId).thenReturn("")
        whenever(authDataManager.createPin(anyString(), anyString())).thenReturn(Completable.error(Throwable()))

        whenever(accessState.pin).thenReturn("")

        // Act
        subject.onPadClicked("7")

        // Assert
        verify(activity).showProgressDialog(anyInt())
        verify(activity, atLeastOnce()).dismissProgressDialog()
        verify(activity).showToast(anyInt(), anyString())
        verify(authDataManager).createPin(anyString(), anyString())
        verify(prefsUtil).clear()
        verify(appUtil).restartApp(LauncherActivity::class.java)
    }

    @Test
    fun padClickedCreatePinWritesNewConfirmationValue() {
        // Arrange
        subject.userEnteredPin = "133"
        whenever(prefsUtil.pinId).thenReturn("")
        whenever(authDataManager.createPin(anyString(), anyString())).thenReturn(Completable.complete())
        whenever(accessState.pin).thenReturn("")

        // Act
        subject.onPadClicked("7")

        // Assert
        assertEquals("1337", subject.userEnteredConfirmationPin)
        assertEquals("", subject.userEnteredPin)
    }

    @Test
    fun padClickedCreatePinMismatched() {
        // Arrange
        subject.userEnteredPin = "133"
        subject.userEnteredConfirmationPin = "1234"
        whenever(prefsUtil.pinId).thenReturn("")
        whenever(authDataManager.createPin(anyString(), anyString())).thenReturn(Completable.complete())
        whenever(accessState.pin).thenReturn("")

        // Act
        subject.onPadClicked("7")

        // Assert
        verify(activity).showToast(anyInt(), anyString())
        verify(activity).dismissProgressDialog()
    }

    @Test
    fun clearPinBoxes() {
        subject.clearPinBoxes()

        verify(activity).clearPinBoxes()
        assertEquals("", subject.userEnteredPin)
    }

    @Test
    fun validatePasswordSuccessful() {
        // Arrange
        val password = "1234567890"
        whenever(payloadManager.initializeAndDecrypt(anyString(), anyString(), eq(password)))
            .thenReturn(Completable.complete())

        // Act
        subject.validatePassword(password)

        // Assert
        verify(payloadManager).initializeAndDecrypt(anyString(), anyString(), eq(password))
        verify(activity).showProgressDialog(anyInt())
        verify(activity).dismissProgressDialog()
        verify(activity).showToast(anyInt(), anyString())
        verify(prefsUtil).removeValue(PersistentPrefs.KEY_PIN_FAILS)
        verify(prefsUtil).pinId = anyString()
        verify(accessState).clearPin()
        verify(activity).restartPageAndClearTop()
    }

    @Test fun validatePasswordThrowsGenericException() {
        // Arrange
        val password = "1234567890"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(Completable.error(Throwable()))

        // Act
        subject.validatePassword(password)

        // Assert
        verify(activity).showProgressDialog(anyInt())
        verify(activity, atLeastOnce()).dismissProgressDialog()
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(activity).showToast(anyInt(), anyString())
        verify(activity).showValidationDialog()
    }

    @Test
    fun validatePasswordThrowsServerConnectionException() {
        // Arrange
        val password = "1234567890"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(Completable.error(ServerConnectionException()))

        // Act
        subject.validatePassword(password)

        // Assert
        verify(activity).showProgressDialog(anyInt())
        verify(activity).dismissProgressDialog()
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(activity).showToast(anyInt(), anyString())
    }

    @Test
    fun validatePasswordThrowsSocketTimeoutException() {
        // Arrange
        val password = "1234567890"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(Completable.error(SocketTimeoutException()))

        // Act
        subject.validatePassword(password)

        // Assert
        verify(activity).showProgressDialog(anyInt())
        verify(activity).dismissProgressDialog()
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(activity).showToast(anyInt(), anyString())
    }

    @Test
    fun validatePasswordThrowsHDWalletExceptionException() {
        // Arrange
        val password = "1234567890"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(Completable.error(HDWalletException()))

        // Act
        subject.validatePassword(password)

        // Assert
        verify(activity).showProgressDialog(anyInt())
        verify(activity).dismissProgressDialog()
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(activity).showToast(anyInt(), anyString())
        verify(appUtil).restartApp(LauncherActivity::class.java)
    }

    @Test
    fun validatePasswordThrowsAccountLockedException() {
        // Arrange
        val password = "1234567890"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(Completable.error(AccountLockedException()))

        // Act
        subject.validatePassword(password)

        // Assert
        verify(activity).showProgressDialog(anyInt())
        verify(activity).dismissProgressDialog()
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(activity).showAccountLockedDialog()
    }

    @Test
    fun updatePayloadInvalidCredentialsException() {
        // Arrange
        val password = "change_me"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(Completable.error(InvalidCredentialsException()))

        // Act
        subject.updatePayload(password, false)

        // Assert
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(activity).showProgressDialog(anyInt())
        verify(activity).dismissProgressDialog()
        verify(activity).goToPasswordRequiredActivity()
        verify(prefsUtil).walletGuid
        verify(prefsUtil).sharedKey

        verifyNoMoreInteractions(prefsUtil)
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun updatePayloadServerConnectionException() {
        // Arrange
        val password = "password"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(Completable.error(ServerConnectionException()))

        val mockPayload: Wallet = mock {
            on { sharedKey } itReturns SHARED_KEY
        }
        whenever(payloadManager.wallet).thenReturn(mockPayload)

        // Act
        subject.updatePayload(password, false)

        // Assert
        verify(activity).showProgressDialog(anyInt())
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(activity).showToast(anyInt(), anyString())
    }

    @Test
    fun updatePayloadDecryptionException() {
        // Arrange
        val password = "password"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(Completable.error(DecryptionException()))

        val mockPayload: Wallet = mock {
            on { sharedKey } itReturns SHARED_KEY
        }
        whenever(payloadManager.wallet).thenReturn(mockPayload)

        // Act
        subject.updatePayload(password, false)

        // Assert
        verify(activity).showProgressDialog(anyInt())
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(activity).goToPasswordRequiredActivity()
    }

    @Test
    fun updatePayloadHDWalletException() {
        // Arrange
        val password = "password"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(Completable.error(HDWalletException()))
        val mockPayload: Wallet = mock {
            on { sharedKey } itReturns SHARED_KEY
        }
        whenever(payloadManager.wallet).thenReturn(mockPayload)

        // Act
        subject.updatePayload(password, false)

        // Assert
        verify(activity).showProgressDialog(anyInt())
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(activity).showToast(anyInt(), anyString())
        verify(appUtil).restartApp(LauncherActivity::class.java)
    }

    @Test
    fun updatePayloadVersionNotSupported() {
        // Arrange
        val password = "password"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(Completable.error(UnsupportedVersionException()))

        val mockPayload: Wallet = mock {
            on { sharedKey } itReturns SHARED_KEY
        }
        whenever(payloadManager.wallet).thenReturn(mockPayload)

        // Act
        subject.updatePayload(password, false)

        // Assert
        verify(activity).showProgressDialog(anyInt())
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(activity).showWalletVersionNotSupportedDialog(isNull())
    }

    @Test
    fun updatePayloadAccountLocked() {
        // Arrange
        val password = "Change_Me"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(Completable.error(AccountLockedException()))

        val mockPayload = mock<Wallet> {
            on { sharedKey } itReturns SHARED_KEY
        }
        whenever(payloadManager.wallet).thenReturn(mockPayload)

        // Act
        subject.updatePayload(password, false)

        // Assert
        verify(activity).showProgressDialog(anyInt())
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(activity).showAccountLockedDialog()
    }

    @Test
    fun updatePayloadSuccessfulSetLabels() {
        // Arrange
        val password = "Change_Me"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(Completable.complete())

        val mockAccount: Account = mock {
            on { label } itReturns ""
        }
        whenever(payloadManager.accounts).thenReturn(listOf(mockAccount))
        whenever(payloadManager.getAccount(0)).thenReturn(mockAccount)

        val mockWallet: Wallet = mock {
            on { sharedKey } itReturns SHARED_KEY
        }
        whenever(payloadManager.wallet).thenReturn(mockWallet)
        whenever(accessState.isNewlyCreated).thenReturn(true)

        // Act
        subject.updatePayload(password, false)

        // Assert
        assertTrue(subject.canShowFingerprintDialog)

        verify(activity).showProgressDialog(anyInt())
        verify(activity).dismissProgressDialog()
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(prefsUtil).walletGuid
        verify(prefsUtil).sharedKey
        verify(prefsUtil).sharedKey = SHARED_KEY
        verify(payloadManager, Mockito.atLeastOnce()).wallet
        verify(defaultLabels).getDefaultNonCustodialWalletLabel(any())
        verify(mockWallet).sharedKey

        verifyNoMoreInteractions(prefsUtil)
        verifyNoMoreInteractions(mockWallet)
    }

    @Test
    fun updatePayloadSuccessfulUpgradeWallet() {
        // Arrange
        val password = "password"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(Completable.complete())

        val mockAccount: Account = mock {
            on { label } itReturns "label"
        }
        whenever(payloadManager.getAccount(0)).thenReturn(mockAccount)
        val mockWallet: Wallet = mock {
            on { sharedKey } itReturns SHARED_KEY
        }
        whenever(payloadManager.wallet).thenReturn(mockWallet)
        whenever(payloadManager.isWalletUpgradeRequired).thenReturn(true)
        whenever(accessState.isNewlyCreated).thenReturn(false)

        // Act
        subject.updatePayload(password, false)

        // Assert
        assertTrue(subject.canShowFingerprintDialog)

        verify(activity).showProgressDialog(anyInt())
        verify(activity).dismissProgressDialog()
        verify(activity).walletUpgradeRequired(anyInt())
        verify(prefsUtil).walletGuid
        verify(prefsUtil).sharedKey
        verify(prefsUtil).sharedKey = SHARED_KEY
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(payloadManager).isWalletUpgradeRequired
        verify(payloadManager, atLeastOnce()).wallet

        verifyNoMoreInteractions(activity)
        verifyNoMoreInteractions(prefsUtil)
        verifyNoMoreInteractions(payloadManager)
    }

    @Test
    fun updatePayloadSuccessfulVerifyPin() {
        // Arrange
        val password = "password"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(Completable.complete())

        val mockAccount: Account = mock {
            on { label } itReturns "label"
        }
        whenever(payloadManager.getAccount(0)).thenReturn(mockAccount)
        whenever(payloadManager.isWalletUpgradeRequired).thenReturn(false)

        val mockWallet: Wallet = mock {
            on { sharedKey } itReturns SHARED_KEY
        }
        whenever(payloadManager.wallet).thenReturn(mockWallet)
        whenever(accessState.isNewlyCreated).thenReturn(false)

        // Act
        subject.updatePayload(password, false)

        // Assert
        assertTrue(subject.canShowFingerprintDialog)

        verify(activity).showProgressDialog(anyInt())
        verify(activity).dismissProgressDialog()
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(payloadManager).isWalletUpgradeRequired
        verify(payloadManager).wallet
        verify(prefsUtil).sharedKey = SHARED_KEY
        verify(activity).restartAppWithVerifiedPin()
        verify(mockWallet).sharedKey

        verifyNoMoreInteractions(mockWallet)
        verifyNoMoreInteractions(payloadManager)
    }

    @Test
    fun incrementFailureCount() {
        // Act
        subject.incrementFailureCountAndRestart()
        // Assert
        verify(prefsUtil).pinFails
        verify(prefsUtil).pinFails = anyInt()
        verify(activity).showToast(anyInt(), anyString())
        verify(activity).restartPageAndClearTop()
    }

    @Test
    fun resetApp() {
        // Act
        subject.resetApp()
        // Assert
        verify(credentialsWiper).wipe()
    }

    @Test
    fun allowExit() {
        // Act
        val allowExit = subject.allowExit()
        // Assert
        assertEquals(subject.allowExit(), allowExit)
    }

    @Test
    fun isCreatingNewPin() {
        whenever(prefsUtil.pinId).thenReturn("")
        // Act
        val creatingNewPin = subject.isCreatingNewPin
        // Assert
        assertTrue(creatingNewPin)
    }

    @Test
    fun isNotCreatingNewPin() {
        // Arrange
        whenever(prefsUtil.pinId).thenReturn("1234567890")
        // Act
        val creatingNewPin = subject.isCreatingNewPin
        // Assert
        assertFalse(creatingNewPin)
    }

    @Test
    fun fetchInfoMessage() {
        // Arrange
        val mobileNoticeDialog = MobileNoticeDialog(
            "title",
            "body",
            "primarybutton",
            "link"
        )
        whenever(mobileNoticeRemoteConfig.mobileNoticeDialog()).thenReturn(Single.just(mobileNoticeDialog))

        // Act
        subject.fetchInfoMessage()
        // Assert
        verify(activity).showMobileNotice(mobileNoticeDialog)
    }

    @Test
    fun checkForceUpgradeStatus_false() {
        // Arrange
        val versionName = "281"
        whenever(walletOptionsDataManager.checkForceUpgrade(versionName))
            .thenReturn(Observable.just(UpdateType.NONE))

        // Act
        subject.checkForceUpgradeStatus(versionName)

        // Assert
        verify(walletOptionsDataManager).checkForceUpgrade(versionName)
        verifyZeroInteractions(activity)
    }

    @Test
    fun checkForceUpgradeStatus_true() {
        // Arrange
        val versionName = "281"
        whenever(walletOptionsDataManager.checkForceUpgrade(versionName))
            .thenReturn(Observable.just(UpdateType.FORCE))

        // Act
        subject.checkForceUpgradeStatus(versionName)

        // Assert
        verify(walletOptionsDataManager).checkForceUpgrade(versionName)
        verify(activity).appNeedsUpgrade(true)
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun finishSignup_success() {
        subject.finishSignupProcess()

        verify(activity).restartAppWithVerifiedPin()
    }

    @Test
    fun handlePayloadUpdateComplete_needsUpgrade() {
        val mockWallet = mock<Wallet>()
        whenever(payloadManager.wallet).thenReturn(mockWallet)
        whenever(mockWallet.sharedKey).thenReturn("")
        whenever(payloadManager.isWalletUpgradeRequired).thenReturn(true)

        subject.handlePayloadUpdateComplete(false)

        verify(activity).walletUpgradeRequired(anyInt())
    }

    @Test
    fun handlePayloadUpdateComplete_fromPinCreationAndBiometricsEnabled() {
        val mockWallet: Wallet = mock {
            on { sharedKey } itReturns SHARED_KEY
        }
        whenever(payloadManager.wallet).thenReturn(mockWallet)
        whenever(biometricsController.isFingerprintAvailable).thenReturn(true)

        subject.handlePayloadUpdateComplete(true)

        verify(activity).askToUseBiometrics()
    }

    @Test
    fun handlePayloadUpdateComplete_fromPinCreationAndBiometricsNotEnabled() {
        val mockWallet: Wallet = mock {
            on { sharedKey } itReturns SHARED_KEY
        }
        whenever(payloadManager.wallet).thenReturn(mockWallet)
        whenever(biometricsController.isFingerprintAvailable).thenReturn(false)

        subject.handlePayloadUpdateComplete(true)

        verify(activity).restartAppWithVerifiedPin()
    }

    @Test
    fun handlePayloadUpdateComplete_notFromPinCreation() {
        val mockWallet: Wallet = mock {
            on { sharedKey } itReturns SHARED_KEY
        }
        whenever(payloadManager.wallet).thenReturn(mockWallet)
        whenever(biometricsController.isFingerprintAvailable).thenReturn(true)

        subject.handlePayloadUpdateComplete(false)

        verify(activity).restartAppWithVerifiedPin()
        verify(mockWallet).sharedKey

        verifyNoMoreInteractions(mockWallet)
    }

    companion object {
        private const val WALLET_GUID = "0000-0000-0000-0000-0000"
        private const val SHARED_KEY = "121212121212"
    }
}