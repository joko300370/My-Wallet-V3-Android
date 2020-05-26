package piuk.blockchain.android.ui.start

import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.payload.data.Wallet

import org.junit.Before
import org.junit.Test
import org.amshove.kluent.any
import org.amshove.kluent.mock
import piuk.blockchain.android.testutils.RxTest
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.android.util.AppUtil
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

class PasswordRequiredPresenterTest : RxTest() {

    private lateinit var subject: PasswordRequiredPresenter
    private val view: PasswordRequiredView = mock()
    private val appUtil: AppUtil = mock()
    private val prefs: PersistentPrefs = mock()
    private val authDataManager: AuthDataManager = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val wallet: Wallet = mock()

    @Before
    fun setUp() {
        whenever(prefs.getValue(PersistentPrefs.KEY_WALLET_GUID, "")).thenReturn(GUID)

        whenever(payloadDataManager.wallet).thenReturn(wallet)
        whenever(payloadDataManager.wallet!!.sharedKey).thenReturn("shared_key")
        whenever(payloadDataManager.wallet!!.guid).thenReturn(GUID)

        subject = PasswordRequiredPresenter(
            appUtil,
            prefs,
            authDataManager,
            payloadDataManager
        )

        subject.attachView(view)
    }

    @Test
    fun onContinueClickedNoPassword() {
        // Arrange
        whenever(prefs.getValue(PersistentPrefs.KEY_WALLET_GUID, "")).thenReturn("")

        // Act
        subject.onContinueClicked("")

        // Assert
        verify(view).showToast(anyInt(), anyString())
    }

    @Test
    fun onForgetWalletClickedShowWarningAndDismiss() {
        // Arrange

        // Act
        subject.onForgetWalletClicked()

        // Assert
        verify(view).showForgetWalletWarning(any())
        verifyNoMoreInteractions(view)
    }

    companion object {
        private const val GUID = "1234567890"
    }
}