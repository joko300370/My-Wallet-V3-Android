package piuk.blockchain.android.ui.settings

import com.blockchain.notifications.NotificationTokenManager
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.models.responses.nabu.KycTierState
import com.blockchain.swap.nabu.models.responses.nabu.NabuApiException.Companion.fromResponseBody
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.settings.SettingsManager
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import org.amshove.kluent.`it returns`
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import piuk.blockchain.android.R
import piuk.blockchain.android.testutils.RxTest
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.android.thepit.PitLinkingState
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper
import piuk.blockchain.android.ui.kyc.settings.KycStatusHelper
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.ui.tiers
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.Email
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import retrofit2.Response.error

class SettingsPresenterTest : RxTest() {
    private lateinit var subject: SettingsPresenter

    private val activity: SettingsView = mock()

    private val fingerprintHelper: FingerprintHelper = mock()
    private val authDataManager: AuthDataManager = mock()

    private val settingsDataManager: SettingsDataManager = mock()

    private val payloadManager: PayloadManager = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val stringUtils: StringUtils = mock()

    private val prefsUtil: PersistentPrefs = mock()
    private val accessState: AccessState = mock()
    private val swipeToReceiveHelper: SwipeToReceiveHelper = mock()

    private val notificationTokenManager: NotificationTokenManager = mock()
    private val exchangeRateDataManager: ExchangeRateDataManager = mock()
    private val kycStatusHelper: KycStatusHelper = mock()
    private val emailSyncUpdater: EmailSyncUpdater = mock()
    private val pitLinking: PitLinking = mock()
    private val pitLinkState: PitLinkingState = mock()

    private val featureFlag: FeatureFlag = mock()

    private val analytics: Analytics = mock()
    private val simpleBuyPrefs: SimpleBuyPrefs = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val cardsFeatureFlag: FeatureFlag = mock()
    private val fundsFeatureFlag: FeatureFlag = mock()
    private val currencyPrefs: CurrencyPrefs = mock()

    @Before
    fun setUp() {
        subject = SettingsPresenter(
            fingerprintHelper = fingerprintHelper,
            authDataManager = authDataManager,
            settingsDataManager = settingsDataManager,
            emailUpdater = emailSyncUpdater,
            payloadManager = payloadManager,
            payloadDataManager = payloadDataManager,
            stringUtils = stringUtils,
            prefs = prefsUtil,
            currencyPrefs = currencyPrefs,
            accessState = accessState,
            custodialWalletManager = custodialWalletManager,
            swipeToReceiveHelper = swipeToReceiveHelper,
            notificationTokenManager = notificationTokenManager,
            exchangeRateDataManager = exchangeRateDataManager,
            kycStatusHelper = kycStatusHelper,
            pitLinking = pitLinking,
            analytics = analytics,
            simpleBuyPrefs = simpleBuyPrefs
        )
        subject.initView(activity)
        whenever(currencyPrefs.selectedFiatCurrency).thenReturn("USD")
        whenever(prefsUtil.arePushNotificationsEnabled).thenReturn(false)
        whenever(fingerprintHelper.isHardwareDetected()).thenReturn(false)
        whenever(prefsUtil.getValue(any(), any<Boolean>())).thenReturn(false)
        whenever(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete())
        whenever(payloadDataManager.syncPayloadAndPublicKeys()).thenReturn(Completable.complete())
    }

    @Test
    fun onViewReadySuccess() {
        // Arrange
        val mockSettings: Settings = mock {
            on { isNotificationsOn } `it returns` true
            on { notificationsType } `it returns` listOf(1, 32)
            on { smsNumber } `it returns` "sms"
            on { email } `it returns` "email"
        }

        whenever(settingsDataManager.fetchSettings()).thenReturn(Observable.just(mockSettings))
        whenever(currencyPrefs.selectedFiatCurrency).thenReturn("USD")
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))
        whenever(kycStatusHelper.getSettingsKycStateTier())
            .thenReturn(Single.just(tiers(KycTierState.None, KycTierState.None)))
        whenever(pitLinkState.isLinked).thenReturn(false)
        whenever(custodialWalletManager.fetchUnawareLimitsCards(ArgumentMatchers.anyList()))
            .thenReturn(Single.just(emptyList()))
        whenever(pitLinking.state).thenReturn(Observable.just(pitLinkState))

        whenever(featureFlag.enabled).thenReturn(Single.just(true))
        whenever(cardsFeatureFlag.enabled).thenReturn(Single.just(true))
        whenever(fundsFeatureFlag.enabled).thenReturn(Single.just(true))
        whenever(custodialWalletManager.getLinkedBeneficiaries()).thenReturn(Single.just(emptyList()))
        whenever(custodialWalletManager.getSupportedFundsFiats(any(), any())).thenReturn(Single.just(emptyList()))
        whenever(custodialWalletManager.updateSupportedCardTypes(ArgumentMatchers.anyString())).thenReturn(
            Completable.complete())
        // Act
        subject.onViewReady()
        // Assert
        Mockito.verify(activity).showProgressDialog(ArgumentMatchers.anyInt())
        Mockito.verify(activity).hideProgressDialog()
        Mockito.verify(activity).setUpUi()
        Mockito.verify(activity).setPitLinkingState(false)
        Mockito.verify(activity, Mockito.times(2)).updateCards(emptyList())
        Mockito.verify(activity, Mockito.times(2))
            .updateBanks(LinkedBanksAndSupportedCurrencies(emptyList(), emptyList()))
    }

    @Test
    fun onViewReadyFailed() {
        // Arrange
        whenever(
            settingsDataManager.fetchSettings()).thenReturn(Observable.error(Throwable()))
        whenever(pitLinkState.isLinked).thenReturn(false)
        whenever(kycStatusHelper.getSettingsKycStateTier())
            .thenReturn(Single.just(tiers(KycTierState.Verified, KycTierState.Verified)))
        whenever(pitLinking.state).thenReturn(Observable.just(pitLinkState))
        whenever(featureFlag.enabled).thenReturn(Single.just(false))
        whenever(currencyPrefs.selectedFiatCurrency).thenReturn("USD")
        whenever(cardsFeatureFlag.enabled).thenReturn(Single.just(false))
        whenever(fundsFeatureFlag.enabled).thenReturn(Single.just(false))
        whenever(custodialWalletManager.getSupportedFundsFiats(any(), any())).thenReturn(Single.just(emptyList()))
        whenever(custodialWalletManager.updateSupportedCardTypes(ArgumentMatchers.anyString())).thenReturn(
            Completable.complete())
        whenever(custodialWalletManager.fetchUnawareLimitsCards(ArgumentMatchers.anyList()))
            .thenReturn(Single.just(emptyList()))
        whenever(custodialWalletManager.getLinkedBeneficiaries()).thenReturn(Single.just(emptyList()))

        // Act
        subject.onViewReady()

        // Assert
        Mockito.verify(activity).showProgressDialog(ArgumentMatchers.anyInt())
        Mockito.verify(activity).hideProgressDialog()
        Mockito.verify(activity).setUpUi()
        Mockito.verify(activity, times(2)).updateCards(emptyList())
        Mockito.verify(activity, Mockito.times(2))
            .updateBanks(LinkedBanksAndSupportedCurrencies(emptyList(), emptyList()))
    }

    @Test
    fun onKycStatusClicked_should_launch_homebrew_tier1() {
        assertClickLaunchesKyc(KycTierState.Verified, KycTierState.None)
    }

    @Test
    fun onKycStatusClicked_should_launch_homebrew_tier2() {
        assertClickLaunchesKyc(KycTierState.Verified, KycTierState.Verified)
    }

    @Test
    fun onKycStatusClicked_should_launch_kyc_flow_locked() {
        assertClickLaunchesKyc(KycTierState.None, KycTierState.None)
    }

    @Test
    fun onKycStatusClicked_should_launch_kyc_status_tier1_review() {
        assertClickLaunchesKyc(KycTierState.Pending, KycTierState.None)
    }

    @Test
    fun onKycStatusClicked_should_launch_kyc_status_tier2_review() {
        assertClickLaunchesKyc(KycTierState.Verified, KycTierState.Pending)
    }

    @Test
    fun onKycStatusClicked_should_launch_kyc_status_tier1_rejected() {
        assertClickLaunchesKyc(KycTierState.Rejected, KycTierState.None)
    }

    @Test
    fun onKycStatusClicked_should_launch_kyc_status_tier2_rejected() {
        assertClickLaunchesKyc(KycTierState.Verified, KycTierState.Rejected)
    }

    @Test
    fun updateEmailSuccess() {
        // Arrange
        val notifications: List<Int> = listOf(SettingsManager.NOTIFICATION_TYPE_EMAIL)

        val mockSettings = Settings().copy(notificationsType = notifications)

        val email = "EMAIL"
        whenever(emailSyncUpdater.updateEmailAndSync(email)).thenReturn(Single.just(Email(email, false)))
        whenever(settingsDataManager.fetchSettings()).thenReturn(Observable.just(mockSettings))
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))
        whenever(settingsDataManager.disableNotification(Settings.NOTIFICATION_TYPE_EMAIL, notifications))
            .thenReturn(Observable.just(mockSettings))
        // Act
        subject.updateEmail(email)
        // Assert
        Mockito.verify(emailSyncUpdater).updateEmailAndSync(email)
        Mockito.verify(settingsDataManager).disableNotification(Settings.NOTIFICATION_TYPE_EMAIL, notifications)
        Mockito.verify(activity).showDialogEmailVerification()
    }

    @Test
    fun updateEmailFailed() {
        // Arrange
        val email = "EMAIL"
        whenever(emailSyncUpdater.updateEmailAndSync(email)).thenReturn(Single.error(Throwable()))
        // Act
        subject.updateEmail(email)
        // Assert
        Mockito.verify(emailSyncUpdater).updateEmailAndSync(email)
        Mockito.verify(activity).showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)
        Mockito.verifyNoMoreInteractions(activity)
    }

    @Test
    fun updateSmsInvalid() {
        // Arrange
        val stringResource = "STRING_RESOURCE"
        whenever(stringUtils.getString(any())).thenReturn(stringResource)

        subject.updateSms("")
        // Assert
        Mockito.verify(activity).setSmsSummary(stringResource)
        Mockito.verifyNoMoreInteractions(activity)
    }

    @Test
    fun updateSmsSuccess() {
        // Arrange
        val notifications: List<Int> = listOf(SettingsManager.NOTIFICATION_TYPE_SMS)

        val mockSettings = Settings().copy(notificationsType = notifications)
        val phoneNumber = "PHONE_NUMBER"
        whenever(settingsDataManager.updateSms(phoneNumber)).thenReturn(Observable.just(mockSettings))
        whenever(settingsDataManager.disableNotification(Settings.NOTIFICATION_TYPE_SMS, notifications))
            .thenReturn(Observable.just(mockSettings))
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))
        whenever(kycStatusHelper.syncPhoneNumberWithNabu()).thenReturn(Completable.complete())
        // Act
        subject.updateSms(phoneNumber)
        // Assert
        Mockito.verify(settingsDataManager).updateSms(phoneNumber)
        Mockito.verify(settingsDataManager).disableNotification(Settings.NOTIFICATION_TYPE_SMS, notifications)
        Mockito.verify(activity).showDialogVerifySms()
    }

    @Test
    fun updateSmsSuccess_despiteNumberAlreadyRegistered() {
        // Arrange
        val notifications: List<Int> = listOf(SettingsManager.NOTIFICATION_TYPE_SMS)

        val mockSettings = Settings().copy(notificationsType = notifications)
        val phoneNumber = "PHONE_NUMBER"
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))
        whenever(
            settingsDataManager.updateSms(phoneNumber)).thenReturn(Observable.just(mockSettings))
        whenever(
            settingsDataManager.disableNotification(Settings.NOTIFICATION_TYPE_SMS, notifications))
            .thenReturn(Observable.just(mockSettings))
        val responseBody = ResponseBody.create("application/json".toMediaTypeOrNull(), "{}")
        val error = fromResponseBody(error<Any>(409, responseBody))
        whenever(kycStatusHelper.syncPhoneNumberWithNabu()).thenReturn(Completable.error(error))
        // Act
        subject.updateSms(phoneNumber)
        // Assert
        Mockito.verify(settingsDataManager).updateSms(phoneNumber)
        Mockito.verify(settingsDataManager).disableNotification(Settings.NOTIFICATION_TYPE_SMS, notifications)
        Mockito.verify(activity).showDialogVerifySms()
    }

    @Test
    fun updateSmsFailed() {
        // Arrange
        val phoneNumber = "PHONE_NUMBER"
        Mockito.`when`(
            settingsDataManager.updateSms(phoneNumber)).thenReturn(Observable.error(Throwable()))
        // Act
        subject.updateSms(phoneNumber)
        // Assert
        Mockito.verify(settingsDataManager).updateSms(phoneNumber)
        Mockito.verifyNoMoreInteractions(settingsDataManager)
        Mockito.verify(activity).showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)
        Mockito.verifyNoMoreInteractions(activity)
    }

    @Test
    fun verifySmsSuccess() {
        // Arrange
        val verificationCode = "VERIFICATION_CODE"
        val mockSettings = Settings()
        whenever(settingsDataManager.verifySms(verificationCode)).thenReturn(Observable.just(mockSettings))
        whenever(kycStatusHelper.syncPhoneNumberWithNabu()).thenReturn(Completable.complete())
        // Act
        subject.verifySms(verificationCode)
        // Assert
        Mockito.verify(settingsDataManager).verifySms(verificationCode)
        Mockito.verifyNoMoreInteractions(settingsDataManager)
        Mockito.verify(activity).showProgressDialog(ArgumentMatchers.anyInt())
        Mockito.verify(activity).hideProgressDialog()
        Mockito.verify(activity).showDialogSmsVerified()
    }

    @Test
    fun verifySmsFailed() {
        // Arrange
        val verificationCode = "VERIFICATION_CODE"
        whenever(settingsDataManager.verifySms(ArgumentMatchers.anyString())).thenReturn(Observable.error(Throwable()))

        // Act
        subject.verifySms(verificationCode)
        // Assert
        Mockito.verify(settingsDataManager).verifySms(verificationCode)
        Mockito.verifyNoMoreInteractions(settingsDataManager)
        Mockito.verify(activity).showProgressDialog(ArgumentMatchers.anyInt())
        Mockito.verify(activity).hideProgressDialog()
        Mockito.verify(activity).showWarningDialog(R.string.verify_sms_failed)
    }

    @Test
    fun updateTorSuccess() {
        // Arrange
        val mockSettings = Settings().copy(
            blockTorIps = 1
        )
        whenever(settingsDataManager.updateTor(true)).thenReturn(Observable.just(mockSettings))

        // Act
        subject.updateTor(true)
        // Assert
        Mockito.verify(settingsDataManager).updateTor(true)
        Mockito.verify(activity).setTorBlocked(true)
    }

    @Test
    fun updateTorFailed() {
        // Arrange
        Mockito.`when`(settingsDataManager.updateTor(true)).thenReturn(Observable.error(Throwable()))
        // Act
        subject.updateTor(true)
        // Assert
        Mockito.verify(settingsDataManager).updateTor(true)
        Mockito.verify(activity).showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)
    }

    @Test
    fun update2FaSuccess() {
        // Arrange
        val mockSettings = Settings()
        val authType = SettingsManager.AUTH_TYPE_YUBI_KEY
        Mockito.`when`(
            settingsDataManager.updateTwoFactor(authType)).thenReturn(Observable.just(mockSettings))
        // Act
        subject.updateTwoFa(authType)
        // Assert
        Mockito.verify(settingsDataManager).updateTwoFactor(authType)
    }

    @Test
    fun update2FaFailed() {
        // Arrange
        val authType = SettingsManager.AUTH_TYPE_YUBI_KEY
        Mockito.`when`(
            settingsDataManager.updateTwoFactor(authType)).thenReturn(Observable.error(Throwable()))

        // Act
        subject.updateTwoFa(authType)
        // Assert
        Mockito.verify(settingsDataManager).updateTwoFactor(authType)
        Mockito.verify(activity).showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)
    }

    @Test
    fun enableNotificationSuccess() {
        // Arrange
        val mockSettingsResponse = Settings()
        val mockSettings = Settings().copy(
            notificationsType = listOf(
                SettingsManager.NOTIFICATION_TYPE_NONE
            )
        )

        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))

        whenever(
            settingsDataManager.enableNotification(SettingsManager.NOTIFICATION_TYPE_EMAIL, listOf(
                SettingsManager.NOTIFICATION_TYPE_NONE
            )))
            .thenReturn(Observable.just(mockSettingsResponse))
        // Act
        subject.updateEmailNotification(true)
        // Assert
        Mockito.verify(settingsDataManager)
            .enableNotification(SettingsManager.NOTIFICATION_TYPE_EMAIL, listOf(
                SettingsManager.NOTIFICATION_TYPE_NONE
            ))
        Mockito.verify(payloadDataManager).syncPayloadAndPublicKeys()
        Mockito.verify(activity).setEmailNotificationPref(true)
    }

    @Test
    fun disableNotificationSuccess() {
        // Arrange

        val mockSettingsResponse = Settings()
        val mockSettings = Settings().copy(
            notificationsType = listOf(SettingsManager.NOTIFICATION_TYPE_EMAIL)
        )

        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))

        Mockito.`when`(
            settingsDataManager.disableNotification(SettingsManager.NOTIFICATION_TYPE_EMAIL,
                listOf(SettingsManager.NOTIFICATION_TYPE_EMAIL))).thenReturn(Observable.just(mockSettingsResponse))
        // Act
        subject.updateEmailNotification(false)
        // Assert
        Mockito.verify(settingsDataManager)
            .disableNotification(SettingsManager.NOTIFICATION_TYPE_EMAIL,
                listOf(SettingsManager.NOTIFICATION_TYPE_EMAIL))

        Mockito.verify(payloadDataManager).syncPayloadWithServer()
        Mockito.verify(activity).setEmailNotificationPref(ArgumentMatchers.anyBoolean())
    }

    @Test
    fun enableNotificationAlreadyEnabled() {
        // Arrange
        val mockSettings = Settings().copy(
            notificationsType = listOf(SettingsManager.NOTIFICATION_TYPE_EMAIL),
            notificationsOn = 1
        )
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))
        // Act
        subject.updateEmailNotification(true)
        // Assert
        Mockito.verify(settingsDataManager).getSettings()
        Mockito.verifyNoMoreInteractions(settingsDataManager)
        Mockito.verify(activity).setEmailNotificationPref(true)
    }

    @Test
    fun disableNotificationAlreadyDisabled() {
        // Assert
        val mockSettings = Settings().copy(
            notificationsType = listOf(SettingsManager.NOTIFICATION_TYPE_NONE),
            notificationsOn = 1
        )
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))
        // Act
        subject.updateEmailNotification(false)
        // Assert
        Mockito.verify(settingsDataManager).getSettings()
        Mockito.verifyNoMoreInteractions(settingsDataManager)
        Mockito.verify(activity).setEmailNotificationPref(false)
    }

    @Test
    fun enableNotificationFailed() {
        // Arrange
        // Arrange
        val mockSettings = Settings().copy(
            notificationsType = listOf(SettingsManager.NOTIFICATION_TYPE_NONE)
        )
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))
        // Act

        whenever(settingsDataManager.enableNotification(SettingsManager.NOTIFICATION_TYPE_EMAIL,
            listOf(SettingsManager.NOTIFICATION_TYPE_NONE))).thenReturn(Observable.error(Throwable()))
        // Act
        subject.updateEmailNotification(true)
        // Assert
        Mockito.verify(settingsDataManager).enableNotification(SettingsManager.NOTIFICATION_TYPE_EMAIL,
            listOf(SettingsManager.NOTIFICATION_TYPE_NONE))
        Mockito.verify(activity).showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)
    }

    @Test
    fun pinCodeValidatedForChange() {
        // Arrange

        // Act
        subject.pinCodeValidatedForChange()
        // Assert
        Mockito.verify(prefsUtil).removeValue(PersistentPrefs.KEY_PIN_FAILS)
        Mockito.verify(prefsUtil).pinId = ""
        Mockito.verify(activity).goToPinEntryPage()
        Mockito.verifyNoMoreInteractions(activity)
    }

    @Test
    fun updatePasswordSuccess() {
        // Arrange
        val newPassword = "NEW_PASSWORD"
        val oldPassword = "OLD_PASSWORD"
        val pin = "PIN"
        Mockito.`when`(accessState.pin).thenReturn(pin)
        Mockito.`when`(authDataManager.createPin(newPassword, pin)).thenReturn(Completable.complete())
        Mockito.`when`(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete())
        // Act
        subject.updatePassword(newPassword, oldPassword)
        // Assert
        Mockito.verify(accessState).pin
        Mockito.verify(authDataManager).createPin(newPassword, pin)
        Mockito.verify(payloadDataManager).syncPayloadWithServer()
        Mockito.verify(activity).showProgressDialog(ArgumentMatchers.anyInt())
        Mockito.verify(activity).hideProgressDialog()
        Mockito.verify(activity).showToast(R.string.password_changed, ToastCustom.TYPE_OK)
    }

    @Test
    fun updatePasswordFailed() {
        // Arrange
        val newPassword = "NEW_PASSWORD"
        val oldPassword = "OLD_PASSWORD"
        val pin = "PIN"
        Mockito.`when`(accessState.pin).thenReturn(pin)
        Mockito.`when`(authDataManager.createPin(newPassword, pin))
            .thenReturn(Completable.error(Throwable()))
        Mockito.`when`(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete())
        // Act
        subject.updatePassword(newPassword, oldPassword)
        // Assert
        Mockito.verify(accessState).pin
        Mockito.verify(authDataManager).createPin(newPassword, pin)
        Mockito.verify(payloadDataManager).syncPayloadWithServer()
        Mockito.verify(payloadManager).tempPassword = newPassword
        Mockito.verify(payloadManager).tempPassword = oldPassword
        Mockito.verify(activity).showProgressDialog(ArgumentMatchers.anyInt())
        Mockito.verify(activity).hideProgressDialog()
        Mockito.verify(activity)
            .showToast(R.string.remote_save_failed, ToastCustom.TYPE_ERROR)
        Mockito.verify(activity)
            .showToast(R.string.password_unchanged, ToastCustom.TYPE_ERROR)
    }

    @Test
    fun storeSwipeToReceiveAddressesSuccessful() {
        // Arrange
        whenever(swipeToReceiveHelper.generateAddresses()).thenReturn(Completable.complete())
        // Act
        subject.storeSwipeToReceiveAddresses()
        testScheduler.triggerActions()
        // Assert
        Mockito.verify(swipeToReceiveHelper).generateAddresses()
        Mockito.verifyNoMoreInteractions(swipeToReceiveHelper)
        Mockito.verify(activity).showProgressDialog(R.string.please_wait)
        Mockito.verify(activity).hideProgressDialog()
        Mockito.verifyNoMoreInteractions(activity)
    }

    @Test
    fun storeSwipeToReceiveAddressesFailed() {
        // Arrange
        whenever(swipeToReceiveHelper.generateAddresses()).thenReturn(Completable.error(Throwable()))
        // Act
        subject.storeSwipeToReceiveAddresses()
        testScheduler.triggerActions()
        // Assert
        Mockito.verify(swipeToReceiveHelper).generateAddresses()
        Mockito.verifyNoMoreInteractions(swipeToReceiveHelper)
        Mockito.verify(activity).showProgressDialog(ArgumentMatchers.anyInt())
        Mockito.verify(activity).hideProgressDialog()
        Mockito.verify(activity).showToast(R.string.update_failed, ToastCustom.TYPE_ERROR)
        Mockito.verifyNoMoreInteractions(activity)
    }

    @Test
    fun clearSwipeToReceiveData() {
        // Arrange

        // Act
        subject.clearSwipeToReceiveData()
        // Assert
        swipeToReceiveHelper.clearStoredData()
    }

    @Test
    fun enablePushNotifications() {
        // Arrange
        whenever(notificationTokenManager.enableNotifications()).thenReturn(Completable.complete())

        // Act
        subject.enablePushNotifications()

        // Assert
        Mockito.verify(activity).setPushNotificationPref(true)
        Mockito.verify(notificationTokenManager).enableNotifications()
        Mockito.verifyNoMoreInteractions(notificationTokenManager)
    }

    @Test
    fun disablePushNotifications() {
        // Arrange
        whenever(notificationTokenManager.disableNotifications()).thenReturn(Completable.complete())

        // Act
        subject.disablePushNotifications()

        // Assert
        Mockito.verify(activity).setPushNotificationPref(false)
        Mockito.verify(notificationTokenManager).disableNotifications()
        Mockito.verifyNoMoreInteractions(notificationTokenManager)
    }

    private fun assertClickLaunchesKyc(status1: KycTierState, status2: KycTierState) {
        // Arrange
        whenever(kycStatusHelper.getKycTierStatus())
            .thenReturn(Single.just(tiers(status1, status2)))
        // Act
        subject.onKycStatusClicked()
        // Assert
        Mockito.verify(activity).launchKycFlow()
    }
}