package piuk.blockchain.android.ui.settings

import android.annotation.SuppressLint
import com.blockchain.notifications.NotificationTokenManager
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.SettingsAnalyticsEvents
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.PaymentMethod
import com.blockchain.swap.nabu.datamanagers.custodialwalletimpl.CardStatus
import com.blockchain.swap.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.swap.nabu.models.responses.nabu.KycTiers
import com.blockchain.swap.nabu.models.responses.nabu.NabuApiException
import com.blockchain.swap.nabu.models.responses.nabu.NabuErrorStatusCodes
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.settings.SettingsManager
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.R
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.android.thepit.PitLinkingState
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper
import piuk.blockchain.android.ui.kyc.settings.KycStatusHelper
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.extensions.thenSingle
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.AndroidUtils
import timber.log.Timber

class SettingsPresenter(
    private val fingerprintHelper: FingerprintHelper,
    private val authDataManager: AuthDataManager,
    private val settingsDataManager: SettingsDataManager,
    private val emailUpdater: EmailSyncUpdater,
    private val payloadManager: PayloadManager,
    private val payloadDataManager: PayloadDataManager,
    private val stringUtils: StringUtils,
    private val prefs: PersistentPrefs,
    private val currencyPrefs: CurrencyPrefs,
    private val accessState: AccessState,
    private val custodialWalletManager: CustodialWalletManager,
    private val swipeToReceiveHelper: SwipeToReceiveHelper,
    private val notificationTokenManager: NotificationTokenManager,
    private val exchangeRateDataManager: ExchangeRateDataManager,
    private val kycStatusHelper: KycStatusHelper,
    private val pitLinking: PitLinking,
    private val analytics: Analytics,
    private val simpleBuyPrefs: SimpleBuyPrefs
) : BasePresenter<SettingsView>() {

    private val fiatUnit: String
        get() = currencyPrefs.selectedFiatCurrency

    override fun onViewReady() {
        view?.showProgressDialog(R.string.please_wait)
        compositeDisposable += settingsDataManager.fetchSettings().singleOrError()
            .zipWith(kycStatusHelper.getSettingsKycStateTier())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { (settings, tiers) ->
                    handleUpdate(settings)
                    view?.setKycState(tiers)
                },
                onError = {
                    handleUpdate(Settings())
                    view?.showToast(R.string.settings_error_updating, ToastCustom.TYPE_ERROR)
                }
            )

        compositeDisposable += pitLinking.state
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { state -> onPitStateUpdated(state) },
                onError = { Timber.e(it) }
            )
        updateCards()
        updateBanks()
    }

    private fun updateCards() {
        compositeDisposable += kycStatusHelper.getSettingsKycStateTier()
            .map { kycTiers: KycTiers -> kycTiers.isApprovedFor(KycTierLevel.GOLD) }
            .doOnSuccess { isGold: Boolean ->
                view?.cardsEnabled(isGold)
            }
            .flatMap { isGold ->
                if (isGold) {
                    custodialWalletManager.updateSupportedCardTypes(fiatUnit).andThen(
                        custodialWalletManager.fetchUnawareLimitsCards(listOf(CardStatus.ACTIVE, CardStatus.EXPIRED)))
                } else {
                    Single.just(emptyList())
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                view?.cardsEnabled(false)
                onCardsUpdated(emptyList())
            }
            .subscribe { cards -> onCardsUpdated(cards) }
    }

    private fun updateBanks() {
        compositeDisposable += kycStatusHelper.getSettingsKycStateTier()
            .map { kycTiers: KycTiers -> kycTiers.isApprovedFor(KycTierLevel.GOLD) }
            .flatMap { isGold: Boolean ->
                supportedCurrencies(fiatUnit, isGold).doOnSuccess {
                    view?.banksEnabled(it.isNotEmpty())
                }.zipWith(custodialWalletManager.getLinkedBeneficiaries()) { supportedCurrencies, linkedBeneficiaries ->
                    LinkedBanksAndSupportedCurrencies(linkedBeneficiaries, supportedCurrencies)
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                view?.banksEnabled(false)
                onBanksUpdated(LinkedBanksAndSupportedCurrencies(emptyList(), emptyList()))
                view?.updateBanks(LinkedBanksAndSupportedCurrencies(emptyList(), emptyList()))
            }
            .subscribe { linkedAndSupportedCurrencies ->
                onBanksUpdated(linkedAndSupportedCurrencies)
            }
    }

    private fun onBanksUpdated(linkedAndSupportedCurrencies: LinkedBanksAndSupportedCurrencies) {
        view?.updateBanks(linkedAndSupportedCurrencies)
    }

    private fun onCardsUpdated(cards: List<PaymentMethod.Card>) {
        view?.updateCards(cards)
    }

    private fun onPitStateUpdated(state: PitLinkingState) {
        view?.setPitLinkingState(state.isLinked)

        pitClickedListener = if (state.isLinked) {
            { view?.launchThePit() }
        } else {
            { view?.launchThePitLandingActivity() }
        }
    }

    private fun supportedCurrencies(fiat: String, isGold: Boolean): Single<List<String>> =
        custodialWalletManager.getSupportedFundsFiats(fiat, isGold)

    fun onKycStatusClicked() {
        view?.launchKycFlow()
    }

    private fun handleUpdate(settings: Settings) {
        view?.hideProgressDialog()
        view?.setUpUi()
        updateUi(settings)
    }

    private fun updateUi(settings: Settings) {
        // GUID
        view?.setGuidSummary(settings.guid)

        // Email
        var emailAndStatus = settings.email
        when {
            emailAndStatus.isEmpty() -> {
                emailAndStatus = stringUtils.getString(R.string.not_specified)
            }
            settings.isEmailVerified -> {
                emailAndStatus += "  (" + stringUtils.getString(R.string.verified) + ")"
            }
            else -> {
                emailAndStatus += "  (" + stringUtils.getString(R.string.unverified) + ")"
            }
        }
        view?.setEmailSummary(emailAndStatus)

        // Phone
        var smsAndStatus = settings.smsNumber
        when {
            smsAndStatus.isEmpty() -> {
                smsAndStatus = stringUtils.getString(R.string.not_specified)
            }
            settings.isSmsVerified -> {
                smsAndStatus += "  (" + stringUtils.getString(R.string.verified) + ")"
            }
            else -> {
                smsAndStatus += "  (" + stringUtils.getString(R.string.unverified) + ")"
            }
        }
        view?.setSmsSummary(smsAndStatus)

        // Fiat
        view?.setFiatSummary(fiatUnit)

        // Email notifications
        view?.setEmailNotificationsVisibility(settings.isEmailVerified)

        // Push and Email notification status
        view?.setEmailNotificationPref(false)
        view?.setPushNotificationPref(arePushNotificationEnabled())
        if (settings.isNotificationsOn && settings.notificationsType.isNotEmpty()) {
            for (type in settings.notificationsType) {
                if (type == Settings.NOTIFICATION_TYPE_EMAIL || type == Settings.NOTIFICATION_TYPE_ALL) {
                    view?.setEmailNotificationPref(true)
                    break
                }
            }
        }

        // Fingerprint
        view?.setFingerprintVisibility(ifFingerprintHardwareAvailable)
        view?.updateFingerprintPreferenceStatus()

        // 2FA
        view?.setTwoFaPreference(settings.authType != Settings.AUTH_TYPE_OFF)

        // Tor
        view?.setTorBlocked(settings.isBlockTorIps)

        // Screenshots
        view?.setScreenshotsEnabled(prefs.getValue(PersistentPrefs.KEY_SCREENSHOTS_ENABLED, false))

        // Launcher shortcuts
        view?.setLauncherShortcutVisibility(AndroidUtils.is25orHigher())
    }

    /**
     * @return true if the device has usable fingerprint hardware
     */
    private val ifFingerprintHardwareAvailable: Boolean
        get() = fingerprintHelper.isHardwareDetected()

    /**
     * @return true if the user has previously enabled fingerprint login
     */
    val ifFingerprintUnlockEnabled: Boolean
        get() = fingerprintHelper.isFingerprintUnlockEnabled()

    /**
     * Sets fingerprint unlock enabled and clears the encrypted PIN if {@param enabled} is false
     *
     * @param enabled Whether or not the fingerprint unlock feature is set up
     */
    fun setFingerprintUnlockEnabled(enabled: Boolean) {
        fingerprintHelper.setFingerprintUnlockEnabled(enabled)
        if (!enabled) {
            fingerprintHelper.clearEncryptedData(PersistentPrefs.KEY_ENCRYPTED_PIN_CODE)
        }
    }

    /**
     * Handle fingerprint preference toggle
     */
    fun onFingerprintClicked() {
        if (ifFingerprintUnlockEnabled) {
            // Show dialog "are you sure you want to disable fingerprint login?
            view?.showDisableFingerprintDialog()
        } else if (!fingerprintHelper.areFingerprintsEnrolled()) {
            // No fingerprints enrolled, prompt user to add some
            view?.showNoFingerprintsAddedDialog()
        } else {
            val pin = accessState.pin
            if (pin.isNotEmpty()) {
                view?.showFingerprintDialog(pin)
            } else {
                throw IllegalStateException("PIN code not found in AccessState")
            }
        }
    }

    private fun String?.isInvalid(): Boolean {
        return this == null || this.isEmpty() || this.length >= 256
    }

    private val cachedSettings = settingsDataManager.getSettings().first(Settings())

    /**
     * @return the temporary password from the Payload Manager
     */
    val tempPassword: String
        get() = payloadManager.tempPassword

    private val authType: Single<Int>
        get() = cachedSettings.map { it.authType }

    /**
     * @return is the user's phone number is verified
     */
    private val isSmsVerified: Single<Boolean>
        get() = cachedSettings.map { it.isSmsVerified }

    /**
     * Write key/value to [android.content.SharedPreferences]
     *
     * @param key The key under which to store the data
     * @param value The value to be stored as a boolean
     */
    fun updatePreferences(key: String, value: Boolean) {
        prefs.setValue(key, value)
    }

    /**
     * Updates the user's email, prompts user to check their email for verification after success
     *
     * @param email The email address to be saved
     */
    fun updateEmail(email: String) {
        if (email.isInvalid()) {
            view?.setEmailSummary(stringUtils.getString(R.string.not_specified))
            return
        }
        compositeDisposable +=
            emailUpdater.updateEmailAndSync(email)
                .flatMap {
                    settingsDataManager.fetchSettings().singleOrError().flatMap {
                        updateNotification(Settings.NOTIFICATION_TYPE_EMAIL, false)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        updateUi(it)
                        view?.showDialogEmailVerification()
                    },
                    onError = { view?.showToast(R.string.update_failed, ToastCustom.TYPE_ERROR) }
                )
    }

    /**
     * Updates the user's phone number, prompts user to verify their number after success
     *
     * @param sms The phone number to be saved
     */
    fun updateSms(sms: String) {
        if (sms.isInvalid()) {
            view?.setEmailSummary(stringUtils.getString(R.string.not_specified))
            return
        }
        compositeDisposable +=
            settingsDataManager.updateSms(sms)
                .firstOrError()
                .flatMap {
                    syncPhoneNumberWithNabu().thenSingle {
                        updateNotification(Settings.NOTIFICATION_TYPE_SMS, false)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        updateUi(it)
                        view?.showDialogVerifySms()
                    },
                    onError = { view?.showToast(R.string.update_failed, ToastCustom.TYPE_ERROR) }
                )
    }

    /**
     * Verifies a user's number, shows verified dialog after success
     *
     * @param code The verification code which has been sent to the user
     */
    fun verifySms(code: String) {
        compositeDisposable +=
            settingsDataManager.verifySms(code)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { updateUi(it) }
                .doOnSubscribe { view?.showProgressDialog(R.string.please_wait) }
                .flatMapCompletable { syncPhoneNumberWithNabu() }
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    view?.hideProgressDialog()
                }
                .subscribeBy(
                    onComplete = { view?.showDialogSmsVerified() },
                    onError = { view?.showWarningDialog(R.string.verify_sms_failed) }
                )
    }

    private fun syncPhoneNumberWithNabu(): Completable {
        return kycStatusHelper.syncPhoneNumberWithNabu()
            .subscribeOn(Schedulers.io())
            .onErrorResumeNext { throwable: Throwable? ->
                if (throwable is NabuApiException && throwable.getErrorStatusCode() ==
                    NabuErrorStatusCodes.AlreadyRegistered
                )
                    Completable.complete()
                else Completable.error(throwable)
            }
    }

    /**
     * Updates the user's Tor blocking preference
     *
     * @param blocked Whether or not to block Tor requests
     */
    fun updateTor(blocked: Boolean) {
        compositeDisposable +=
            settingsDataManager.updateTor(blocked)
                .subscribeBy(
                    onNext = { updateUi(it) },
                    onError = { view?.showToast(R.string.update_failed, ToastCustom.TYPE_ERROR) }
                )
    }

    /**
     * Sets the auth type used for 2FA. Pass in [Settings.AUTH_TYPE_OFF] to disable 2FA
     *
     * @param type The auth type used for 2FA
     * @see Settings
     */
    fun updateTwoFa(type: Int) {
        compositeDisposable +=
            settingsDataManager.updateTwoFactor(type)
                .subscribeBy(
                    onNext = { updateUi(it) },
                    onError = { view?.showToast(R.string.update_failed, ToastCustom.TYPE_ERROR) }
                )
    }

    /**
     * Updates the user's notification preferences. Will not make any web requests if not necessary.
     *
     * @param type The notification type to be updated
     * @param enable Whether or not to enable the notification type
     * @see Settings
     */

    fun updateNotification(type: Int, enable: Boolean): Single<Settings> {
        return cachedSettings.flatMap {
            if (enable && it.isNotificationTypeEnabled(type)) {
                // No need to change
                return@flatMap Single.just(it)
            } else if (!enable && it.isNotificationTypeDisabled(type)) {
                // No need to change
                return@flatMap Single.just(it)
            }
            val notificationsUpdate =
                if (enable) settingsDataManager.enableNotification(type, it.notificationsType)
                else settingsDataManager.disableNotification(type, it.notificationsType)
            notificationsUpdate.flatMapCompletable {
                if (enable) {
                    payloadDataManager.syncPayloadAndPublicKeys()
                } else {
                    payloadDataManager.syncPayloadWithServer()
                }
            }.thenSingle {
                cachedSettings
            }
        }
    }

    private fun Settings.isNotificationTypeEnabled(type: Int): Boolean {
        return isNotificationsOn && (notificationsType.contains(type) ||
                notificationsType.contains(SettingsManager.NOTIFICATION_TYPE_ALL))
    }

    private fun Settings.isNotificationTypeDisabled(type: Int): Boolean {
        return notificationsType.contains(SettingsManager.NOTIFICATION_TYPE_NONE) ||
                (!notificationsType.contains(SettingsManager.NOTIFICATION_TYPE_ALL) &&
                        !notificationsType.contains(type))
    }

    /**
     * PIN code validated, take user to PIN change page
     */
    fun pinCodeValidatedForChange() {
        prefs.removeValue(PersistentPrefs.KEY_PIN_FAILS)
        prefs.pinId = ""
        view?.goToPinEntryPage()
    }

    /**
     * Updates the user's password
     *
     * @param password The requested new password as a String
     * @param fallbackPassword The user's current password as a fallback
     */
    @SuppressLint("CheckResult")
    fun updatePassword(password: String, fallbackPassword: String) {
        payloadManager.tempPassword = password
        compositeDisposable += authDataManager.createPin(password, accessState.pin)
            .doOnSubscribe { view?.showProgressDialog(R.string.please_wait) }
            .doOnTerminate { view?.hideProgressDialog() }
            .andThen(payloadDataManager.syncPayloadWithServer())
            .subscribeBy(
                onComplete = {
                    view?.showToast(R.string.password_changed, ToastCustom.TYPE_OK)
                    analytics.logEvent(SettingsAnalyticsEvents.PasswordChanged)
                },
                onError = { showUpdatePasswordFailed(fallbackPassword) })
    }

    private fun showUpdatePasswordFailed(fallbackPassword: String) {
        payloadManager.tempPassword = fallbackPassword
        view?.showToast(R.string.remote_save_failed, ToastCustom.TYPE_ERROR)
        view?.showToast(R.string.password_unchanged, ToastCustom.TYPE_ERROR)
    }

    /**
     * Updates the user's fiat unit preference
     */
    fun updateFiatUnit(fiatUnit: String) {
        compositeDisposable +=
            settingsDataManager.updateFiatUnit(fiatUnit)
                .subscribeBy(
                    onNext = {
                        if (currencyPrefs.selectedFiatCurrency != fiatUnit)
                            analytics.logEvent(AnalyticsEvents.ChangeFiatCurrency)
                        prefs.selectedFiatCurrency = fiatUnit
                        simpleBuyPrefs.clearState()
                        analytics.logEvent(SettingsAnalyticsEvents.CurrencyChanged)
                        updateUi(it)
                    },
                    onError = { view?.showToast(R.string.update_failed, ToastCustom.TYPE_ERROR) })
    }

    fun storeSwipeToReceiveAddresses() {
        compositeDisposable +=
            swipeToReceiveHelper.generateAddresses()
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe { view?.showProgressDialog(R.string.please_wait) }
                .doOnTerminate { view?.hideProgressDialog() }
                .subscribeBy(
                    onComplete = {},
                    onError = {
                        view?.showToast(R.string.update_failed,
                            ToastCustom.TYPE_ERROR)
                    })
    }

    fun clearSwipeToReceiveData() {
        swipeToReceiveHelper.clearStoredData()
    }

    fun updateCloudData(newValue: Boolean) {
        if (newValue) {
            swipeToReceiveHelper.clearStoredData()
        }
        prefs.backupEnabled = newValue
    }

    private fun arePushNotificationEnabled(): Boolean {
        return prefs.arePushNotificationsEnabled
    }

    fun enablePushNotifications() {
        compositeDisposable += notificationTokenManager.enableNotifications()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = { view?.setPushNotificationPref(true) },
                onError = { Timber.e(it) }
            )
    }

    fun disablePushNotifications() {
        compositeDisposable += notificationTokenManager.disableNotifications()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = { view?.setPushNotificationPref(false) },
                onError = { Timber.e(it) }
            )
    }

    val currencyLabels: Array<String>
        get() = exchangeRateDataManager.getCurrencyLabels()

    fun onThePitClicked() {
        pitClickedListener()
    }

    fun onTwoStepVerificationRequested() {
        compositeDisposable += authType.zipWith(isSmsVerified).subscribe { (auth, smsVerified) ->
            view?.showDialogTwoFA(auth, smsVerified)
        }
    }

    fun resendSms() {
        compositeDisposable += cachedSettings.subscribeBy {
            updateSms(it.smsNumber)
        }
    }

    fun onVerifySmsRequested() {
        compositeDisposable += cachedSettings.subscribeBy {
            view?.showDialogMobile(it.authType, it.isSmsVerified, it.smsNumber ?: "")
        }
    }

    fun onEmailShowRequested() {
        compositeDisposable += cachedSettings.subscribeBy {
            view?.showEmailDialog(it.email, it.isEmailVerified)
        }
    }

    private var pitClickedListener = {}
}