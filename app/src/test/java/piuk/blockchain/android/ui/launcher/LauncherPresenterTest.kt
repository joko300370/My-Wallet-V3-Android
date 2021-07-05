package piuk.blockchain.android.ui.launcher

import android.app.LauncherActivity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.blockchain.logging.CrashLogger
import com.blockchain.notifications.NotificationTokenManager
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.remoteconfig.FeatureFlag
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.payload.data.Wallet
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.itReturns
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import piuk.blockchain.android.BlockchainTestApplication
import piuk.blockchain.android.R
import piuk.blockchain.android.identity.UserIdentity
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs

@Config(sdk = [23], application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class LauncherPresenterTest {
    private val launcherActivity: LauncherView = mock()
    private val prefsUtil: PersistentPrefs = mock()
    private val appUtil: AppUtil = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val deepLinkPersistence: DeepLinkPersistence = mock()
    private val settingsDataManager: SettingsDataManager = mock()
    private val accessState: AccessState = mock()
    private val intent: Intent = mock()
    private val extras: Bundle = mock()
    private val wallet: Wallet = mock()
    private val notificationTokenManager: NotificationTokenManager = mock()
    private val environmentConfig: EnvironmentConfig = mock()
    private val featureFlag: FeatureFlag = mock()
    private val userIdentity: UserIdentity = mock()
    private val currencyPrefs: CurrencyPrefs = mock {
        on { selectedFiatCurrency } itReturns SELECTED_FIAT
        on { defaultFiatCurrency } itReturns DEFAULT_FIAT
    }
    private val analytics: Analytics = mock()
    private val crashLogger: CrashLogger = mock()
    private val prerequisites: Prerequisites = mock()

    private val subject = LauncherPresenter(
        appUtil,
        payloadDataManager,
        prefsUtil,
        deepLinkPersistence,
        accessState,
        settingsDataManager,
        notificationTokenManager,
        environmentConfig,
        currencyPrefs,
        analytics,
        prerequisites,
        userIdentity,
        crashLogger
    )

    @Before
    fun setUp() {
        subject.initView(launcherActivity)

        whenever(featureFlag.enabled).thenReturn(Single.just(false))
        val settings: Settings = mock()
        whenever(settingsDataManager.updateFiatUnit(anyString()))
            .thenReturn(Observable.just(settings))
    }

    @Test
    fun onViewReadyVerifiedEmailVerified() {
        // Arrange
        whenever(launcherActivity.getPageIntent()).thenReturn(intent)
        whenever(intent.extras).thenReturn(extras)
        whenever(extras.containsKey(AppUtil.INTENT_EXTRA_VERIFIED)).thenReturn(true)
        whenever(extras.getBoolean(AppUtil.INTENT_EXTRA_VERIFIED)).thenReturn(true)
        whenever(prefsUtil.pinId).thenReturn("1234567890")
        whenever(prefsUtil.walletGuid).thenReturn(WALLET_GUID)
        whenever(prefsUtil.isLoggedOut).thenReturn(false)
        whenever(appUtil.isSane).thenReturn(true)
        whenever(payloadDataManager.wallet).thenReturn(wallet)
        whenever(prerequisites.initMetadataAndRelatedPrerequisites()).thenReturn(Completable.complete())

        val mockSettings: Settings = mock()
        whenever(prerequisites.initSettings(anyString(), anyString())).thenReturn(Single.just(mockSettings))
        whenever(accessState.isLoggedIn).thenReturn(true)

        whenever(wallet.guid).thenReturn(WALLET_GUID)
        whenever(wallet.sharedKey).thenReturn(SHARED_KEY)
        whenever(mockSettings.isEmailVerified).thenReturn(true)
        whenever(mockSettings.currency).thenReturn("USD")
        whenever(notificationTokenManager.resendNotificationToken()).thenReturn(Completable.complete())

        // Act
        subject.onViewReady()

        // Assert
        verify(launcherActivity).onStartMainActivity(null, false)
    }

    /**
     * Everything is good, email not verified and getting [Settings] object failed. Should
     * re-request PIN code.
     */
    @Test
    fun onViewReadyNonVerifiedEmailSettingsFailure() {
        // Arrange
        whenever(launcherActivity.getPageIntent()).thenReturn(intent)
        whenever(intent.extras).thenReturn(extras)
        whenever(extras.containsKey(AppUtil.INTENT_EXTRA_VERIFIED)).thenReturn(true)
        whenever(extras.getBoolean(AppUtil.INTENT_EXTRA_VERIFIED)).thenReturn(true)

        whenever(prefsUtil.pinId).thenReturn("1234567890")
        whenever(prefsUtil.walletGuid).thenReturn(WALLET_GUID)
        whenever(prefsUtil.isLoggedOut).thenReturn(false)
        whenever(appUtil.isSane).thenReturn(true)
        whenever(payloadDataManager.wallet).thenReturn(wallet)
        whenever(accessState.isLoggedIn).thenReturn(true)
        whenever(prerequisites.initMetadataAndRelatedPrerequisites()).thenReturn(Completable.complete())
        whenever(prerequisites.initSettings(anyString(), anyString())).thenReturn(Single.error(Throwable()))
        whenever(wallet.guid).thenReturn(WALLET_GUID)
        whenever(wallet.sharedKey).thenReturn(SHARED_KEY)

        // Act
        subject.onViewReady()

        // Assert
        verify(launcherActivity).showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
        verify(launcherActivity).onRequestPin()
    }

    /**
     * Bitcoin URI is found, expected to step into Bitcoin branch and call [ ][LauncherActivity.onStartMainActivity]
     */
    @Test
    fun onViewReadyBitcoinUri() {
        // Arrange
        whenever(launcherActivity.getPageIntent()).thenReturn(intent)
        whenever(intent.action).thenReturn(Intent.ACTION_VIEW)
        whenever(intent.scheme).thenReturn("bitcoin")
        whenever(intent.data).thenReturn(Uri.parse("bitcoin uri"))
        whenever(intent.extras).thenReturn(extras)
        whenever(extras.containsKey(AppUtil.INTENT_EXTRA_VERIFIED)).thenReturn(true)
        whenever(extras.getBoolean(AppUtil.INTENT_EXTRA_VERIFIED)).thenReturn(true)
        whenever(prefsUtil.walletGuid).thenReturn(WALLET_GUID)
        whenever(prefsUtil.pinId).thenReturn("1234567890")
        whenever(prefsUtil.isLoggedOut).thenReturn(false)
        whenever(appUtil.isSane).thenReturn(true)
        whenever(payloadDataManager.wallet).thenReturn(wallet)
        whenever(accessState.isLoggedIn).thenReturn(true)

        whenever(prerequisites.initMetadataAndRelatedPrerequisites()).thenReturn(Completable.complete())

        val mockSettings: Settings = mock()
        whenever(prerequisites.initSettings(anyString(), anyString())).thenReturn(Single.just(mockSettings))

        whenever(wallet.guid).thenReturn(WALLET_GUID)
        whenever(wallet.sharedKey).thenReturn(SHARED_KEY)
        whenever(mockSettings.isEmailVerified).thenReturn(true)
        whenever(mockSettings.currency).thenReturn("USD")
        whenever(notificationTokenManager.resendNotificationToken()).thenReturn(Completable.complete())

        // Act
        subject.onViewReady()

        // Assert
        verify(prefsUtil).setValue(PersistentPrefs.KEY_SCHEME_URL, "bitcoin uri")
        verify(launcherActivity).onStartMainActivity(null, false)
    }

    /**
     * Everything is fine, but PIN not validated.
     */
    @Test
    fun onViewReadyNotVerified() {
        // Arrange
        whenever(launcherActivity.getPageIntent()).thenReturn(intent)
        whenever(intent.extras).thenReturn(extras)
        whenever(extras.containsKey(AppUtil.INTENT_EXTRA_VERIFIED)).thenReturn(false)
        whenever(prefsUtil.walletGuid).thenReturn(WALLET_GUID)
        whenever(prefsUtil.pinId).thenReturn("1234567890")
        whenever(prefsUtil.isLoggedOut).thenReturn(false)
        whenever(appUtil.isSane).thenReturn(true)
        whenever(payloadDataManager.wallet).thenReturn(wallet)
        whenever(accessState.isLoggedIn).thenReturn(false)

        // Act
        subject.onViewReady()

        // Assert
        verify(launcherActivity).onRequestPin()
    }

    /**
     * Everything is fine, but PIN not validated. However, [AccessState] returns logged in.
     */
    @Test
    fun onViewReadyPinNotValidatedButLoggedIn() {
        // Arrange
        whenever(launcherActivity.getPageIntent()).thenReturn(intent)
        whenever(intent.extras).thenReturn(extras)
        whenever(extras.containsKey(AppUtil.INTENT_EXTRA_VERIFIED)).thenReturn(false)
        whenever(prefsUtil.walletGuid).thenReturn(WALLET_GUID)
        whenever(prefsUtil.pinId).thenReturn("1234567890")
        whenever(prefsUtil.isLoggedOut).thenReturn(false)
        whenever(appUtil.isSane).thenReturn(true)
        whenever(payloadDataManager.wallet).thenReturn(wallet)
        whenever(accessState.isLoggedIn).thenReturn(true)
        whenever(prerequisites.initMetadataAndRelatedPrerequisites()).thenReturn(Completable.complete())
        val mockSettings: Settings = mock()
        whenever(prerequisites.initSettings(WALLET_GUID, SHARED_KEY)).thenReturn(Single.just(mockSettings))

        whenever(wallet.guid).thenReturn(WALLET_GUID)
        whenever(wallet.sharedKey).thenReturn(SHARED_KEY)
        whenever(mockSettings.isEmailVerified).thenReturn(true)
        whenever(mockSettings.currency).thenReturn("USD")
        whenever(notificationTokenManager.resendNotificationToken()).thenReturn(Completable.complete())

        // Act
        subject.onViewReady()

        // Assert
        verify(accessState).isLoggedIn = true
        verify(launcherActivity).onStartMainActivity(null, false)
    }

    /**
     * GUID not found
     */
    @Test
    fun onViewReadyNoGuid() {
        // Arrange
        whenever(launcherActivity.getPageIntent()).thenReturn(intent)
        whenever(intent.extras).thenReturn(extras)
        whenever(extras.containsKey(AppUtil.INTENT_EXTRA_VERIFIED)).thenReturn(false)
        whenever(prefsUtil.walletGuid).thenReturn("")

        // Act
        subject.onViewReady()

        // Assert
        verify(launcherActivity).onNoGuid()
    }

    /**
     * Pin not found
     */
    @Test
    fun onViewReadyNoPin() {
        // Arrange
        whenever(launcherActivity.getPageIntent()).thenReturn(intent)
        whenever(intent.extras).thenReturn(extras)
        whenever(extras.containsKey(AppUtil.INTENT_EXTRA_VERIFIED)).thenReturn(false)
        whenever(prefsUtil.walletGuid).thenReturn(WALLET_GUID)
        whenever(prefsUtil.pinId).thenReturn("")

        // Act
        subject.onViewReady()

        // Assert
        verify(launcherActivity).onRequestPin()
    }

    @Test
    fun onViewReadyNotSane() {
        // Arrange
        whenever(launcherActivity.getPageIntent()).thenReturn(intent)
        whenever(intent.extras).thenReturn(extras)
        whenever(extras.containsKey(AppUtil.INTENT_EXTRA_VERIFIED)).thenReturn(false)
        whenever(prefsUtil.pinId).thenReturn("1234")
        whenever(prefsUtil.walletGuid).thenReturn(WALLET_GUID)
        whenever(appUtil.isSane).thenReturn(false)

        // Act
        subject.onViewReady()

        // Assert
        verify(launcherActivity).onCorruptPayload()
    }

    /**
     * GUID exists, Shared Key exists but user logged out.
     */
    @Test
    fun onViewReadyUserLoggedOut() {
        // Arrange
        whenever(launcherActivity.getPageIntent()).thenReturn(intent)
        whenever(intent.extras).thenReturn(extras)
        whenever(extras.containsKey(AppUtil.INTENT_EXTRA_VERIFIED)).thenReturn(false)
        whenever(prefsUtil.isLoggedOut).thenReturn(true)
        whenever(prefsUtil.walletGuid).thenReturn(WALLET_GUID)

        // Act
        subject.onViewReady()

        // Assert
        verify(launcherActivity).onReEnterPassword()
    }

    @Test
    fun clearCredentialsAndRestart() {
        // Arrange

        // Act
        subject.clearCredentialsAndRestart()
        // Assert
        verify(appUtil).clearCredentialsAndRestart(LauncherActivity::class.java)
    }

    companion object {
        private const val WALLET_GUID = "0000-0000-0000-0000-0000"
        private const val SHARED_KEY = "123123123"

        private const val SELECTED_FIAT = "USD"
        private const val DEFAULT_FIAT = "USD"
    }
}
