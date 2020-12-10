package piuk.blockchain.android.ui.settings

import android.annotation.SuppressLint
import android.util.Pair
import androidx.annotation.VisibleForTesting
import com.blockchain.notifications.NotificationTokenManager
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.SettingsAnalyticsEvents
import com.blockchain.preferences.CurrencyPrefs.selectedFiatCurrency
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.swap.nabu.datamanagers.Beneficiary
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
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.R
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.android.thepit.PitLinkingState
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper
import piuk.blockchain.android.ui.kyc.settings.KycStatusHelper
import piuk.blockchain.android.ui.settings.SettingsFragment.LinkedBanksAndSupportedCurrencies
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.Email
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.AndroidUtils
import timber.log.Timber
import java.util.*

class SettingsPresenter     // Show dialog "are you sure you want to disable fingerprint login?
    (
    private val fingerprintHelper: FingerprintHelper,
    private val authDataManager: AuthDataManager,
    private val settingsDataManager: SettingsDataManager,
    private val emailUpdater: EmailSyncUpdater,
    private val payloadManager: PayloadManager,
    private val payloadDataManager: PayloadDataManager,
    private val stringUtils: StringUtils,
    private val prefs: PersistentPrefs,
    private val accessState: AccessState,
    private val custodialWalletManager: CustodialWalletManager,
    private val swipeToReceiveHelper: SwipeToReceiveHelper,
    private val notificationTokenManager: NotificationTokenManager,
    private val exchangeRateDataManager: ExchangeRateDataManager,
    private val kycStatusHelper: KycStatusHelper,
    private val pitLinking: PitLinking,
    private val analytics: Analytics,
    private val simpleBuyPrefs: SimpleBuyPrefs
) : BasePresenter<SettingsView?>() {
    @VisibleForTesting
    var settings: Settings? = null
    private var pitLinkState = PitLinkingState()
    override fun onViewReady() {
        view!!.showProgressDialog(R.string.please_wait)
        // Fetch updated settings
        compositeDisposable.add(
            settingsDataManager.fetchSettings()
                .doAfterTerminate { handleUpdate() }
                .doOnNext { ignored: Settings? -> loadKyc2TierState() }
                .subscribe(
                    { updatedSettings: Settings? -> settings = updatedSettings }
                ) { throwable: Throwable? ->
                    if (settings == null) {
                        // Show unloaded if necessary, keep old settings if failed update
                        settings = Settings()
                    }
                    // Warn error when updating
                    view!!.showToast(R.string.settings_error_updating, ToastCustom.TYPE_ERROR)
                })
        updateCards()
        updateBanks()
        compositeDisposable.add(pitLinking.state.subscribe({ state: PitLinkingState -> onPitStateUpdated(state) }) { throwable: Throwable? -> })
    }

    private fun updateCards() {
        compositeDisposable.add(
            kycStatusHelper.getSettingsKycStateTier()
                .map { kycTiers: KycTiers -> kycTiers.isApprovedFor(KycTierLevel.GOLD) }
                .doOnSuccess { enabled: Boolean? ->
                    view!!.cardsEnabled(
                        enabled!!)
                }
                .flatMap { enabled: Boolean ->
                    if (enabled) {
                        return@flatMap custodialWalletManager.updateSupportedCardTypes(fiatUnits).andThen(
                            custodialWalletManager.fetchUnawareLimitsCards(
                                Arrays.asList(CardStatus.ACTIVE, CardStatus.EXPIRED)))
                    } else {
                        return@flatMap Single.just(emptyList<PaymentMethod.Card>())
                    }
                }
                .observeOn(AndroidSchedulers.mainThread()).doOnSubscribe { disposable: Disposable? ->
                    view!!.cardsEnabled(false)
                    onCardsUpdated(emptyList())
                    view!!.updateBanks(LinkedBanksAndSupportedCurrencies(emptyList(), emptyList()))
                }
                .subscribe { cards: List<PaymentMethod.Card> -> onCardsUpdated(cards) })
    }

    private fun updateBanks() {
        compositeDisposable.add(
            kycStatusHelper.getSettingsKycStateTier()
                .map { kycTiers: KycTiers -> kycTiers.isApprovedFor(KycTierLevel.GOLD) }
                .flatMap { isGold: Boolean ->
                    canLinkBank(
                        fiatUnits, isGold).doOnSuccess { linkDetails: Pair<Boolean?, List<String?>?> ->
                        view!!.banksEnabled(
                            linkDetails.first!!)
                    }.zipWith<List<Beneficiary?>, LinkedBanksAndSupportedCurrencies>(
                        custodialWalletManager.getLinkedBeneficiaries(),
                        BiFunction { linkDetails: Pair<Boolean?, List<String?>?>, linkedBanks: List<Beneficiary?>? ->
                            LinkedBanksAndSupportedCurrencies(linkedBanks,
                                linkDetails.second)
                        })
                }
                .observeOn(AndroidSchedulers.mainThread()).doOnSubscribe { disposable: Disposable? ->
                    view!!.banksEnabled(false)
                    onBanksUpdated(LinkedBanksAndSupportedCurrencies(emptyList(), emptyList()))
                    view!!.updateBanks(LinkedBanksAndSupportedCurrencies(emptyList(), emptyList()))
                }
                .subscribe { linkedAndSupportedCurrencies: LinkedBanksAndSupportedCurrencies ->
                    onBanksUpdated(linkedAndSupportedCurrencies)
                })
    }

    private fun canLinkBank(fiat: String, isGold: Boolean): Single<Pair<Boolean?, List<String?>?>> {
        return custodialWalletManager.getSupportedFundsFiats(fiat, isGold)
            .map { supportedCurrencies: List<String?> -> Pair(supportedCurrencies.size > 0, supportedCurrencies) }
    }

    private fun onBanksUpdated(linkedAndSupportedCurrencies: LinkedBanksAndSupportedCurrencies) {
        view!!.updateBanks(linkedAndSupportedCurrencies)
    }

    private fun onPitStateUpdated(state: PitLinkingState) {
        pitLinkState = state
        view!!.setPitLinkingState(pitLinkState.isLinked)
    }

    private fun onCardsUpdated(cards: List<PaymentMethod.Card>) {
        view!!.updateCards(cards)
    }

    private fun loadKyc2TierState() {
        compositeDisposable.add(
            kycStatusHelper.getSettingsKycStateTier()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { settingsKycState: KycTiers? -> view!!.setKycState(settingsKycState!!) }) { t: Throwable? ->
                    Timber.e(t)
                }
        )
    }

    fun onKycStatusClicked() {
        view!!.launchKycFlow()
    }

    private fun handleUpdate() {
        view!!.hideProgressDialog()
        view!!.setUpUi()
        updateUi()
    }

    private fun updateUi() {
        // GUID
        view!!.setGuidSummary(settings!!.guid)

        // Email
        var emailAndStatus = settings!!.email
        if (emailAndStatus == null || emailAndStatus.isEmpty()) {
            emailAndStatus = stringUtils.getString(R.string.not_specified)
        } else if (settings!!.isEmailVerified) {
            emailAndStatus += "  (" + stringUtils.getString(R.string.verified) + ")"
        } else {
            emailAndStatus += "  (" + stringUtils.getString(R.string.unverified) + ")"
        }
        view!!.setEmailSummary(emailAndStatus)

        // Phone
        var smsAndStatus = settings!!.smsNumber
        if (smsAndStatus == null || smsAndStatus.isEmpty()) {
            smsAndStatus = stringUtils.getString(R.string.not_specified)
        } else if (settings!!.isSmsVerified) {
            smsAndStatus += "  (" + stringUtils.getString(R.string.verified) + ")"
        } else {
            smsAndStatus += "  (" + stringUtils.getString(R.string.unverified) + ")"
        }
        view!!.setSmsSummary(smsAndStatus)

        // Fiat
        view!!.setFiatSummary(fiatUnits)

        // Email notifications
        view!!.setEmailNotificationsVisibility(settings!!.isEmailVerified)

        // Push and Email notification status
        view!!.setEmailNotificationPref(false)
        view!!.setPushNotificationPref(arePushNotificationEnabled())
        if (settings!!.isNotificationsOn && !settings!!.notificationsType.isEmpty()) {
            for (type in settings!!.notificationsType) {
                if (type == Settings.NOTIFICATION_TYPE_EMAIL || type == Settings.NOTIFICATION_TYPE_ALL) {
                    view!!.setEmailNotificationPref(true)
                    break
                }
            }
        }

        // Fingerprint
        view!!.setFingerprintVisibility(ifFingerprintHardwareAvailable)
        view!!.updateFingerprintPreferenceStatus()

        // 2FA
        view!!.setTwoFaPreference(settings!!.authType != Settings.AUTH_TYPE_OFF)

        // Tor
        view!!.setTorBlocked(settings!!.isBlockTorIps)

        // Screenshots
        view!!.setScreenshotsEnabled(prefs.getValue(PersistentPrefs.KEY_SCREENSHOTS_ENABLED, false))

        // Launcher shortcuts
        view!!.setLauncherShortcutVisibility(AndroidUtils.is25orHigher())
    }

    /**
     * @return true if the device has usable fingerprint hardware
     */
    val ifFingerprintHardwareAvailable: Boolean
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
            view!!.showDisableFingerprintDialog()
        } else if (!fingerprintHelper.areFingerprintsEnrolled()) {
            // No fingerprints enrolled, prompt user to add some
            view!!.showNoFingerprintsAddedDialog()
        } else {
            val pin = accessState.pin
            if (pin != null && !pin.isEmpty()) {
                view!!.showFingerprintDialog(pin)
            } else {
                throw IllegalStateException("PIN code not found in AccessState")
            }
        }
    }

    private fun isInvalidString(string: String?): Boolean {
        return string == null || string.isEmpty() || string.length >= 256
    }

    /**
     * @return the temporary password from the Payload Manager
     */
    val tempPassword: String
        get() = payloadManager.tempPassword

    /**
     * @return the user's email or an empty string if not set
     */
    val email: String
        get() = if (settings!!.email != null) settings!!.email else ""

    /**
     * @return the user's phone number or an empty string if not set
     */
    val sms: String
        get() = if (settings!!.smsNumber != null) settings!!.smsNumber else ""

    /**
     * @return is the user's phone number is verified
     */
    val isSmsVerified: Boolean
        get() = settings!!.isSmsVerified
    val isEmailVerified: Boolean
        get() = settings!!.isEmailVerified

    /**
     * @return the current auth type
     * @see Settings
     */
    val authType: Int
        get() = settings!!.authType

    /**
     * Write key/value to [android.content.SharedPreferences]
     *
     * @param key   The key under which to store the data
     * @param value The value to be stored as a String
     */
    fun updatePreferences(key: String?, value: String?) {
        prefs.setValue(key!!, value!!)
        updateUi()
    }

    /**
     * Write key/value to [android.content.SharedPreferences]
     *
     * @param key   The key under which to store the data
     * @param value The value to be stored as an int
     */
    fun updatePreferences(key: String?, value: Int) {
        prefs.setValue(key!!, value)
        updateUi()
    }

    /**
     * Write key/value to [android.content.SharedPreferences]
     *
     * @param key   The key under which to store the data
     * @param value The value to be stored as a boolean
     */
    fun updatePreferences(key: String?, value: Boolean) {
        prefs.setValue(key!!, value)
        updateUi()
    }

    /**
     * Updates the user's email, prompts user to check their email for verification after success
     *
     * @param email The email address to be saved
     */
    fun updateEmail(email: String?) {
        if (isInvalidString(email)) {
            view!!.setEmailSummary(stringUtils.getString(R.string.not_specified))
        } else {
            compositeDisposable.add(
                emailUpdater.updateEmailAndSync(email!!)
                    .flatMap { e: Email? -> settingsDataManager.fetchSettings().singleOrError() }
                    .subscribe({ settings: Settings? ->
                        this.settings = settings
                        updateNotification(Settings.NOTIFICATION_TYPE_EMAIL, false)
                        view!!.showDialogEmailVerification()
                    }) { throwable: Throwable? -> view!!.showToast(R.string.update_failed, ToastCustom.TYPE_ERROR) })
        }
    }

    /**
     * Updates the user's phone number, prompts user to verify their number after success
     *
     * @param sms The phone number to be saved
     */
    fun updateSms(sms: String?) {
        if (isInvalidString(sms)) {
            view!!.setSmsSummary(stringUtils.getString(R.string.not_specified))
        } else {
            compositeDisposable.add(
                settingsDataManager.updateSms(sms!!)
                    .doOnNext { settings: Settings? -> this.settings = settings }
                    .flatMapCompletable { ignored: Settings? -> syncPhoneNumberWithNabu() }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        updateNotification(Settings.NOTIFICATION_TYPE_SMS, false)
                        view!!.showDialogVerifySms()
                    }) { throwable: Throwable? -> view!!.showToast(R.string.update_failed, ToastCustom.TYPE_ERROR) })
        }
    }

    /**
     * Verifies a user's number, shows verified dialog after success
     *
     * @param code The verification code which has been sent to the user
     */
    fun verifySms(code: String) {
        view!!.showProgressDialog(R.string.please_wait)
        compositeDisposable.add(
            settingsDataManager.verifySms(code)
                .doOnNext { settings: Settings? -> this.settings = settings }
                .flatMapCompletable { ignored: Settings? -> syncPhoneNumberWithNabu() }
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    view!!.hideProgressDialog()
                    updateUi()
                }
                .subscribe(
                    { view!!.showDialogSmsVerified() }
                ) { throwable: Throwable? -> view!!.showWarningDialog(R.string.verify_sms_failed) })
    }

    private fun syncPhoneNumberWithNabu(): Completable {
        return kycStatusHelper.syncPhoneNumberWithNabu()
            .onErrorResumeNext { throwable: Throwable? ->
                if (throwable is NabuApiException) {
                    if (throwable.getErrorStatusCode() === NabuErrorStatusCodes.AlreadyRegistered) {
                        return@onErrorResumeNext Completable.complete()
                    }
                }
                Completable.error(throwable)
            }
    }

    /**
     * Updates the user's Tor blocking preference
     *
     * @param blocked Whether or not to block Tor requests
     */
    fun updateTor(blocked: Boolean) {
        compositeDisposable.add(
            settingsDataManager.updateTor(blocked)
                .doAfterTerminate { updateUi() }
                .subscribe(
                    { settings: Settings? -> this.settings = settings }
                ) { throwable: Throwable? -> view!!.showToast(R.string.update_failed, ToastCustom.TYPE_ERROR) })
    }

    /**
     * Sets the auth type used for 2FA. Pass in [Settings.AUTH_TYPE_OFF] to disable 2FA
     *
     * @param type The auth type used for 2FA
     * @see Settings
     */
    fun updateTwoFa(type: Int) {
        compositeDisposable.add(
            settingsDataManager.updateTwoFactor(type)
                .doAfterTerminate { updateUi() }
                .subscribe(
                    { settings: Settings? -> this.settings = settings }
                ) { throwable: Throwable? -> view!!.showToast(R.string.update_failed, ToastCustom.TYPE_ERROR) })
    }

    /**
     * Updates the user's notification preferences. Will not make any web requests if not necessary.
     *
     * @param type   The notification type to be updated
     * @param enable Whether or not to enable the notification type
     * @see Settings
     */
    fun updateNotification(type: Int, enable: Boolean) {
        if (enable && isNotificationTypeEnabled(type)) {
            // No need to change
            updateUi()
            return
        } else if (!enable && isNotificationTypeDisabled(type)) {
            // No need to change
            updateUi()
            return
        }
        compositeDisposable.add(
            Observable.just(enable)
                .flatMap { aBoolean: Boolean ->
                    if (aBoolean) {
                        return@flatMap settingsDataManager.enableNotification(type, settings!!.notificationsType)
                    } else {
                        return@flatMap settingsDataManager.disableNotification(type, settings!!.notificationsType)
                    }
                }
                .doOnNext { settings: Settings? -> this.settings = settings }
                .flatMapCompletable { ignored: Settings? ->
                    if (enable) {
                        return@flatMapCompletable payloadDataManager.syncPayloadAndPublicKeys()
                    } else {
                        return@flatMapCompletable payloadDataManager.syncPayloadWithServer()
                    }
                }
                .doAfterTerminate { updateUi() }
                .subscribe(
                    {}
                ) { throwable: Throwable? -> view!!.showToast(R.string.update_failed, ToastCustom.TYPE_ERROR) })
    }

    private fun isNotificationTypeEnabled(type: Int): Boolean {
        return (settings!!.isNotificationsOn
                && (settings!!.notificationsType.contains(type)
                || settings!!.notificationsType.contains(SettingsManager.NOTIFICATION_TYPE_ALL)))
    }

    private fun isNotificationTypeDisabled(type: Int): Boolean {
        return (settings!!.notificationsType.contains(SettingsManager.NOTIFICATION_TYPE_NONE)
                || (!settings!!.notificationsType.contains(SettingsManager.NOTIFICATION_TYPE_ALL)
                && !settings!!.notificationsType.contains(type)))
    }

    /**
     * PIN code validated, take user to PIN change page
     */
    fun pinCodeValidatedForChange() {
        prefs.removeValue(PersistentPrefs.KEY_PIN_FAILS)
        prefs.pinId = ""
        view!!.goToPinEntryPage()
    }

    /**
     * Updates the user's password
     *
     * @param password         The requested new password as a String
     * @param fallbackPassword The user's current password as a fallback
     */
    @SuppressLint("CheckResult")
    fun updatePassword(password: String, fallbackPassword: String) {
        payloadManager.tempPassword = password
        authDataManager.createPin(password, accessState.pin)
            .doOnSubscribe { ignored: Disposable? -> view!!.showProgressDialog(R.string.please_wait) }
            .doOnTerminate { view!!.hideProgressDialog() }
            .andThen(payloadDataManager.syncPayloadWithServer())
            .compose(RxUtil.addCompletableToCompositeDisposable(this))
            .subscribe(
                {
                    view!!.showToast(R.string.password_changed, ToastCustom.TYPE_OK)
                    analytics.logEvent(SettingsAnalyticsEvents.PasswordChanged)
                }
            ) { throwable: Throwable? -> showUpdatePasswordFailed(fallbackPassword) }
    }

    private fun showUpdatePasswordFailed(fallbackPassword: String) {
        payloadManager.tempPassword = fallbackPassword
        view!!.showToast(R.string.remote_save_failed, ToastCustom.TYPE_ERROR)
        view!!.showToast(R.string.password_unchanged, ToastCustom.TYPE_ERROR)
    }

    /**
     * Updates the user's fiat unit preference
     */
    fun updateFiatUnit(fiatUnit: String) {
        compositeDisposable.add(
            settingsDataManager.updateFiatUnit(fiatUnit)
                .doAfterTerminate { updateUi() }
                .subscribe(
                    { settings: Settings? ->
                        if (prefs.selectedFiatCurrency == fiatUnit) analytics.logEvent(AnalyticsEvents.ChangeFiatCurrency)
                        prefs.selectedFiatCurrency = fiatUnit
                        simpleBuyPrefs.clearState()
                        this.settings = settings
                        analytics.logEvent(SettingsAnalyticsEvents.CurrencyChanged)
                    }
                ) { throwable: Throwable? -> view!!.showToast(R.string.update_failed, ToastCustom.TYPE_ERROR) })
    }

    fun storeSwipeToReceiveAddresses() {
        compositeDisposable.add(
            swipeToReceiveHelper.generateAddresses()
                .subscribeOn(Schedulers.computation())
                .doOnSubscribe { disposable: Disposable? -> view!!.showProgressDialog(R.string.please_wait) }
                .doOnTerminate { view!!.hideProgressDialog() }
                .subscribe({}) { throwable: Throwable? ->
                    view!!.showToast(R.string.update_failed,
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

    fun arePushNotificationEnabled(): Boolean {
        return prefs.arePushNotificationsEnabled
    }

    fun enablePushNotifications() {
        notificationTokenManager.enableNotifications()
            .compose(RxUtil.addCompletableToCompositeDisposable(this))
            .doOnComplete { view!!.setPushNotificationPref(true) }
            .subscribe({}) { t: Throwable? -> Timber.e(t) }
    }

    fun disablePushNotifications() {
        notificationTokenManager.disableNotifications()
            .compose(RxUtil.addCompletableToCompositeDisposable(this))
            .doOnComplete { view!!.setPushNotificationPref(false) }
            .subscribe({}) { t: Throwable? -> Timber.e(t) }
    }

    val currencyLabels: Array<String>
        get() = exchangeRateDataManager.getCurrencyLabels()

    fun onThePitClicked() {
        if (pitLinkState.isLinked) {
            view!!.launchThePit()
        } else {
            view!!.launchThePitLandingActivity()
        }
    }
}