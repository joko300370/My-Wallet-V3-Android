package piuk.blockchain.android.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ShortcutManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.Html
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.SettingsAnalyticsEvents
import com.blockchain.notifications.analytics.SimpleBuyAnalytics
import com.blockchain.notifications.analytics.linkBankEventWithCurrency
import com.blockchain.swap.nabu.datamanagers.LinkedBank
import com.blockchain.swap.nabu.datamanagers.PaymentMethod
import com.blockchain.swap.nabu.models.nabu.KycTiers
import com.blockchain.ui.dialog.MaterialProgressDialog
import com.blockchain.ui.urllinks.URL_PRIVACY_POLICY
import com.blockchain.ui.urllinks.URL_TOS_POLICY
import com.mukesh.countrypicker.fragments.CountryPicker
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.util.FormatsUtil
import info.blockchain.wallet.util.PasswordUtil
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import org.koin.android.ext.android.inject
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.R.string.success
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.cards.CardDetailsActivity
import piuk.blockchain.android.cards.RemoveCardBottomSheet
import piuk.blockchain.android.simplebuy.RemoveLinkedBankBottomSheet
import piuk.blockchain.android.simplebuy.RemovePaymentMethodBottomSheetHost
import piuk.blockchain.android.ui.auth.KEY_VALIDATING_PIN_FOR_RESULT
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.android.ui.auth.REQUEST_CODE_VALIDATE_PIN
import piuk.blockchain.android.ui.dashboard.sheets.LinkBankAccountDetailsBottomSheet
import piuk.blockchain.android.ui.fingerprint.FingerprintDialog
import piuk.blockchain.android.ui.fingerprint.FingerprintStage
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.settings.preferences.BankPreference
import piuk.blockchain.android.ui.settings.preferences.CardPreference
import piuk.blockchain.android.ui.settings.preferences.KycStatusPreference
import piuk.blockchain.android.ui.settings.preferences.ThePitStatusPreference
import piuk.blockchain.android.ui.thepit.PitLaunchBottomDialog
import piuk.blockchain.android.ui.thepit.PitPermissionsActivity
import piuk.blockchain.android.util.RootUtil
import piuk.blockchain.androidcore.data.events.ActionEvent
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.AndroidUtils
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.helperfunctions.AfterTextChangedWatcher
import piuk.blockchain.androidcoreui.utils.logging.Logging

class SettingsFragment : PreferenceFragmentCompat(), SettingsView, RemovePaymentMethodBottomSheetHost {

    // Profile
    private val kycStatusPref by lazy {
        findPreference<KycStatusPreference>("identity_verification")
    }
    private val guidPref by lazy {
        findPreference<Preference>("guid")
    }
    private val emailPref by lazy {
        findPreference<Preference>("email")
    }
    private val smsPref by lazy {
        findPreference<Preference>("mobile")
    }
    private val thePit by lazy {
        findPreference<ThePitStatusPreference>("the_pit")
    }
    private val banksPref by lazy {
        findPreference<PreferenceCategory>("banks")
    }
    private val cardsPref by lazy {
        findPreference<PreferenceCategory>("cards")
    }

    // Preferences
    private val fiatPref by lazy {
        findPreference<Preference>("fiat")
    }
    private val emailNotificationPref by lazy {
        findPreference<Preference>("email_notifications") as SwitchPreferenceCompat
    }
    private val pushNotificationPref by lazy {
        findPreference<Preference>("push_notifications") as SwitchPreferenceCompat
    }

    // Security
    private val fingerprintPref by lazy {
        findPreference<Preference>("fingerprint") as SwitchPreferenceCompat
    }
    private val twoStepVerificationPref by lazy {
        findPreference<SwitchPreferenceCompat>("2fa")
    }
    private val torPref by lazy {
        findPreference<SwitchPreferenceCompat>("tor")
    }
    private val launcherShortcutPrefs by lazy {
        findPreference<SwitchPreferenceCompat>("receive_shortcuts_enabled")
    }
    private val swipeToReceivePrefs by lazy {
        findPreference<SwitchPreferenceCompat>("swipe_to_receive_enabled")
    }
    private val screenshotPref by lazy {
        findPreference<SwitchPreferenceCompat>("screenshots_enabled")
    }

    private val settingsPresenter: SettingsPresenter by scopedInject()
    private val analytics: Analytics by inject()
    private val rxBus: RxBus by inject()

    private var pwStrength = 0
    private var progressDialog: MaterialProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsPresenter.initView(this)
        settingsPresenter.onViewReady()

        analytics.logEvent(AnalyticsEvents.Settings)
        Logging.logContentView(javaClass.simpleName)
    }

    override fun setUpUi() {
        // Profile
        kycStatusPref.onClick {
            settingsPresenter.onKycStatusClicked()
            analytics.logEvent(SettingsAnalyticsEvents.SwapLimitChecked)
        }
        kycStatusPref?.isVisible = false

        guidPref.onClick {
            showDialogGuid()
            analytics.logEvent(SettingsAnalyticsEvents.WalletIdCopyClicked)
        }
        emailPref.onClick {
            onUpdateEmailClicked()
            analytics.logEvent(SettingsAnalyticsEvents.EmailClicked)
        }
        smsPref.onClick {
            showDialogMobile()
            analytics.logEvent(SettingsAnalyticsEvents.PhoneClicked)
        }

        thePit.onClick { settingsPresenter.onThePitClicked() }

        // Preferences
        fiatPref.onClick { showDialogFiatUnits() }
        emailNotificationPref.onClick {
            showDialogEmailNotifications()
            analytics.logEvent(SettingsAnalyticsEvents.EmailNotificationClicked)
        }
        pushNotificationPref.onClick { showDialogPushNotifications() }

        // Security
        fingerprintPref.onClick {
            onFingerprintClicked()
            analytics.logEvent(SettingsAnalyticsEvents.BiometryAuthSwitch)
        }
        findPreference<Preference>("pin").onClick {
            showDialogChangePin()
            analytics.logEvent(SettingsAnalyticsEvents.ChangePinClicked)
        }
        twoStepVerificationPref.onClick {
            showDialogTwoFA()
            analytics.logEvent(SettingsAnalyticsEvents.TwoFactorAuthClicked)
        }

        findPreference<Preference>("change_pw").onClick {
            showDialogChangePasswordWarning()
            analytics.logEvent(SettingsAnalyticsEvents.ChangePassClicked)
        }

        torPref?.setOnPreferenceChangeListener { _, newValue ->
            settingsPresenter.updateTor(newValue as Boolean)
            true
        }

        screenshotPref?.setOnPreferenceChangeListener { _, newValue ->
            settingsPresenter.updatePreferences(
                PersistentPrefs.KEY_SCREENSHOTS_ENABLED,
                newValue as Boolean
            )
            true
        }

        launcherShortcutPrefs?.setOnPreferenceChangeListener { _, newValue ->
            if (!(newValue as Boolean) && AndroidUtils.is25orHigher()) {
                activity!!.getSystemService(
                    ShortcutManager::class.java
                )!!.removeAllDynamicShortcuts()
            }
            true
        }

        swipeToReceivePrefs?.setOnPreferenceChangeListener { _, newValue ->
            if (!(newValue as Boolean)) {
                settingsPresenter.clearSwipeToReceiveData()
            } else {
                AlertDialog.Builder(activity!!, R.style.AlertDialogStyle)
                    .setTitle(R.string.swipe_receive_hint)
                    .setMessage(R.string.swipe_receive_address_info)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        settingsPresenter.storeSwipeToReceiveAddresses()
                    }
                    .setCancelable(false)
                    .show()
            }
            analytics.logEvent(SettingsAnalyticsEvents.SwipeToReceiveSwitch)
            true
        }

        // App
        findPreference<Preference>("about")?.apply {
            summary = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) ${BuildConfig.COMMIT_HASH}"
            onClick { onAboutClicked() }
        }

        findPreference<Preference>("tos").onClick { onTosClicked() }
        findPreference<Preference>("privacy").onClick { onPrivacyClicked() }

        val disableRootWarningPref = findPreference<Preference>(PersistentPrefs.KEY_ROOT_WARNING_DISABLED)
        if (disableRootWarningPref != null && !RootUtil().isDeviceRooted) {
            val appCategory = findPreference<Preference>("app") as PreferenceCategory
            appCategory.removePreference(disableRootWarningPref)
        }

        // Check if referred from Security Centre dialog
        val intent = activity?.intent
        when {
            intent == null -> {
            }
            intent.hasExtra(EXTRA_SHOW_TWO_FA_DIALOG) ->
                showDialogTwoFA()
            intent.hasExtra(EXTRA_SHOW_ADD_EMAIL_DIALOG) ->
                showUpdateEmailDialog(activity!!, settingsPresenter)
        }
    }

    override fun showProgressDialog(@StringRes message: Int) {
        progressDialog = MaterialProgressDialog(requireContext()).apply {
            setCancelable(false)
            setMessage(message)
            show()
        }
    }

    override fun isPitEnabled(enabled: Boolean) {
        thePit?.isVisible = enabled
    }

    override fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    override fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String) {
        ToastCustom.makeText(activity, getString(message), ToastCustom.LENGTH_SHORT, toastType)
    }

    override fun showWarningDialog(@StringRes message: Int) {
        AlertDialog.Builder(activity!!, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setCancelable(true)
            .setPositiveButton(android.R.string.ok, null)
            .setOnDismissListener { showDialogVerifySms() }
            .create()
            .show()
    }

    private val compositeDisposable = CompositeDisposable()

    private val event by unsafeLazy {
        rxBus.register(ActionEvent::class.java)
    }

    override fun onResume() {
        super.onResume()

        compositeDisposable += event.subscribe {
            settingsPresenter.onViewReady()
        }
    }

    override fun onPause() {
        rxBus.unregister(ActionEvent::class.java, event)
        compositeDisposable.clear()
        super.onPause()
    }

    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        preferenceScreen?.removeAll()
        addPreferencesFromResource(R.xml.settings)
    }

    override fun setKycState(kycTiers: KycTiers) {
        kycStatusPref?.setValue(kycTiers)
        kycStatusPref?.isVisible = kycTiers.isInitialised()
    }

    override fun setGuidSummary(summary: String) {
        guidPref?.summary = summary
    }

    override fun setEmailSummary(summary: String) {
        emailPref?.summary = summary
    }

    override fun setSmsSummary(summary: String) {
        smsPref?.summary = summary
    }

    override fun setFiatSummary(summary: String) {
        fiatPref?.summary = summary
    }

    override fun setEmailNotificationsVisibility(visible: Boolean) {
        emailNotificationPref.isVisible = visible
    }

    override fun setEmailNotificationPref(enabled: Boolean) {
        emailNotificationPref.isChecked = enabled
    }

    override fun setPushNotificationPref(enabled: Boolean) {
        pushNotificationPref.isChecked = enabled
    }

    override fun setFingerprintVisibility(visible: Boolean) {
        fingerprintPref.isVisible = visible
    }

    override fun setTwoFaPreference(enabled: Boolean) {
        twoStepVerificationPref?.isChecked = enabled
    }

    override fun setTorBlocked(blocked: Boolean) {
        torPref?.isChecked = blocked
    }

    override fun setPitLinkingState(isLinked: Boolean) {
        thePit?.setValue(isLinked)
    }

    override fun updateBanks(banks: List<LinkedBank>) {
        val existingBanks = prefsExistingBanks()

        val newBanks = banks.filterNot { existingBanks.contains(it.id) }

        newBanks.forEach { bank ->
            banksPref?.addPreference(
                BankPreference(context = requireContext(), bank = bank, fiatCurrency = bank.currency).apply {
                    onClick {
                        removeBank(bank)
                    }
                    key = bank.id
                }
            )
        }

        addOrUpdateLinkBankForCurrencies(banks.size + 1, listOf("GBP", "EUR"))
    }

    private fun addOrUpdateLinkBankForCurrencies(firstIndex: Int, currencies: List<String>) {

        currencies.forEach { currency ->
            banksPref?.findPreference<BankPreference>(LINK_BANK_KEY.plus(currency))?.let {
                it.order = it.order + firstIndex + currencies.indexOf(currency)
            } ?: banksPref?.addPreference(
                BankPreference(context = requireContext(), fiatCurrency = currency).apply {
                    onClick {
                        linkBankWithCurrency(currency)
                    }
                    key = LINK_BANK_KEY.plus(currency)
                }
            )
        }
    }

    private fun removeBank(bank: LinkedBank) {
        RemoveLinkedBankBottomSheet.newInstance(bank).show(childFragmentManager, "BOTTOM_SHEET")
    }

    private fun linkBankWithCurrency(currency: String) {
        LinkBankAccountDetailsBottomSheet.newInstance(currency).show(childFragmentManager, "BOTTOM_SHEET")
        analytics.logEvent(linkBankEventWithCurrency(SimpleBuyAnalytics.LINK_BANK_CLICKED, currency))
    }

    override fun updateCards(cards: List<PaymentMethod.Card>) {

        val existingCards = prefsExistingCards()

        val newCards = cards.filterNot { existingCards.contains(it.cardId) }

        newCards.forEach { card ->
            cardsPref?.addPreference(
                CardPreference(context = requireContext(), card = card).apply {
                    onClick {
                        RemoveCardBottomSheet.newInstance(card).show(childFragmentManager, "BOTTOM_SHEET")
                    }
                    key = card.cardId
                }
            )
        }

        cardsPref?.findPreference<CardPreference>(ADD_CARD_KEY)?.let {
            it.order = it.order + newCards.size + 1
        } ?: cardsPref?.addPreference(
            CardPreference(context = requireContext(), card = PaymentMethod.Undefined).apply {
                onClick {
                    addNewCard()
                }
                key = ADD_CARD_KEY
            }
        )
    }

    private fun prefsExistingCards(): List<String> {
        val existingCards = mutableListOf<String>()

        for (i in (0 until (cardsPref?.preferenceCount ?: 0))) {
            existingCards.add(cardsPref?.getPreference(i)?.key.takeIf { it != ADD_CARD_KEY } ?: continue)
        }
        return existingCards
    }

    private fun addNewCard() {
        val intent = Intent(activity, CardDetailsActivity::class.java)
        startActivityForResult(intent, CardDetailsActivity.ADD_CARD_REQUEST_CODE)
        analytics.logEvent(SimpleBuyAnalytics.SETTINGS_ADD_CARD)
    }

    override fun setScreenshotsEnabled(enabled: Boolean) {
        screenshotPref?.isChecked = enabled
    }

    private fun prefsExistingBanks(): List<String> {
        val existingBanks = mutableListOf<String>()

        for (i in (0 until (banksPref?.preferenceCount ?: 0))) {
            existingBanks.add(banksPref?.getPreference(i)?.key.takeIf { it?.contains(LINK_BANK_KEY)?.not() ?: false }
                ?: continue)
        }
        return existingBanks
    }

    override fun setLauncherShortcutVisibility(visible: Boolean) {
        launcherShortcutPrefs?.isVisible = visible
    }

    private fun onFingerprintClicked() {
        settingsPresenter.onFingerprintClicked()
    }

    override fun showDisableFingerprintDialog() {
        AlertDialog.Builder(activity!!, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(R.string.fingerprint_disable_message)
            .setCancelable(true)
            .setPositiveButton(R.string.common_yes) { _, _ ->
                settingsPresenter.setFingerprintUnlockEnabled(
                    false
                )
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                updateFingerprintPreferenceStatus()
            }
            .show()
    }

    override fun showNoFingerprintsAddedDialog() {
        updateFingerprintPreferenceStatus()
        AlertDialog.Builder(activity!!, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(R.string.fingerprint_no_fingerprints_added)
            .setCancelable(true)
            .setPositiveButton(R.string.common_yes) { _, _ ->
                startActivityForResult(
                    Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS),
                    0
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun updateFingerprintPreferenceStatus() {
        fingerprintPref.isChecked = settingsPresenter.ifFingerprintUnlockEnabled
    }

    override fun showFingerprintDialog(pincode: String) {
        val dialog = FingerprintDialog.newInstance(pincode, FingerprintStage.REGISTER_FINGERPRINT)
        dialog.setAuthCallback(object : FingerprintDialog.FingerprintAuthCallback {
            override fun onAuthenticated(data: String?) {
                dialog.dismissAllowingStateLoss()
                settingsPresenter.setFingerprintUnlockEnabled(true)
            }

            override fun onCanceled() {
                dialog.dismissAllowingStateLoss()
                settingsPresenter.setFingerprintUnlockEnabled(false)
                fingerprintPref.isChecked = settingsPresenter.ifFingerprintUnlockEnabled
            }
        })
        dialog.show(fragmentManager!!, FingerprintDialog.TAG)
    }

    override fun showDialogSmsVerified() {
        AlertDialog.Builder(activity!!, R.style.AlertDialogStyle)
            .setTitle(success)
            .setMessage(R.string.sms_verified)
            .setPositiveButton(R.string.dialog_continue) { _, _ -> showDialogTwoFA() }
            .show()
    }

    override fun goToPinEntryPage() {
        val intent = Intent(activity, PinEntryActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        analytics.logEvent(SettingsAnalyticsEvents.PinChanged)
    }

    override fun launchThePitLandingActivity() {
        PitPermissionsActivity.start(requireActivity(), "")
    }

    override fun launchThePit() {
        PitLaunchBottomDialog.launch(requireActivity())
    }

    private fun onUpdateEmailClicked() {
        showUpdateEmailDialog(requireActivity(), settingsPresenter)
    }

    private fun onAboutClicked() {
        val aboutDialog = AboutDialog()
        aboutDialog.show(fragmentManager!!, "ABOUT_DIALOG")
    }

    private fun onTosClicked() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_TOS_POLICY)))
    }

    private fun onPrivacyClicked() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_PRIVACY_POLICY)))
    }

    override fun showDialogEmailVerification() {
        // Slight delay to prevent UI blinking issues
        val handler = Handler()
        handler.postDelayed({
            if (activity != null) {
                AlertDialog.Builder(activity!!, R.style.AlertDialogStyle)
                    .setTitle(R.string.verify)
                    .setMessage(R.string.verify_email_notice)
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }, 300)
    }

    private fun showDialogMobile() {
        if (settingsPresenter.authType != Settings.AUTH_TYPE_OFF) {
            AlertDialog.Builder(activity!!, R.style.AlertDialogStyle)
                .setTitle(R.string.warning)
                .setMessage(R.string.disable_2fa_first)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show()
        } else {
            val inflater = activity!!.layoutInflater
            val smsPickerView = inflater.inflate(R.layout.include_sms_update, null)
            val mobileNumber = smsPickerView.findViewById<EditText>(R.id.etSms)
            val countryTextView = smsPickerView.findViewById<TextView>(R.id.tvCountry)
            val mobileNumberTextView = smsPickerView.findViewById<TextView>(R.id.tvSms)

            val picker = CountryPicker.newInstance(getString(R.string.select_country))
            val country = picker.getUserCountryInfo(activity!!)
            if (country.dialCode == "+93") {
                setCountryFlag(countryTextView, "+1", R.drawable.flag_us)
            } else {
                setCountryFlag(countryTextView, country.dialCode, country.flag)
            }

            countryTextView.setOnClickListener {
                picker.show(fragmentManager!!, "COUNTRY_PICKER")
                picker.setListener { _, _, dialCode, flagDrawableResID ->
                    setCountryFlag(countryTextView, dialCode, flagDrawableResID)
                    picker.dismiss()
                }
            }

            if (settingsPresenter.sms.isNotEmpty()) {
                mobileNumberTextView.text = settingsPresenter.sms
                mobileNumberTextView.visibility = View.VISIBLE
            }

            val alertDialogSmsBuilder = AlertDialog.Builder(activity!!, R.style.AlertDialogStyle)
                .setTitle(R.string.mobile)
                .setMessage(getString(R.string.mobile_description))
                .setView(smsPickerView)
                .setCancelable(false)
                .setPositiveButton(R.string.update, null)
                .setNegativeButton(android.R.string.cancel, null)

            if (!settingsPresenter.isSmsVerified && settingsPresenter.sms.isNotEmpty()) {
                alertDialogSmsBuilder.setNeutralButton(R.string.verify) { dialogInterface, i ->
                    settingsPresenter.updateSms(
                        settingsPresenter.sms
                    )
                }
            }

            val dialog = alertDialogSmsBuilder.create()
            dialog.setOnShowListener {
                val positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                positive.setOnClickListener {
                    val sms = countryTextView.text.toString() + mobileNumber.text.toString()

                    if (!FormatsUtil.isValidMobileNumber(sms)) {
                        showCustomToast(R.string.invalid_mobile)
                    } else {
                        settingsPresenter.updateSms(sms)
                        dialog.dismiss()
                    }
                }
            }

            dialog.show()
        }
    }

    private fun showDialogGuid() {
        AlertDialog.Builder(activity!!, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(R.string.guid_to_clipboard)
            .setCancelable(false)
            .setPositiveButton(R.string.common_yes) { _, _ ->
                val clipboard =
                    activity!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("guid", guidPref!!.summary)
                clipboard.primaryClip = clip
                showCustomToast(R.string.copied_to_clipboard)
                analytics.logEvent(SettingsAnalyticsEvents.WalletIdCopyCopied)
            }
            .setNegativeButton(R.string.common_no, null)
            .show()
    }

    private fun showDialogFiatUnits() {
        val currencies = settingsPresenter.currencyLabels
        val strCurrency = settingsPresenter.fiatUnits
        var selected = 0
        for (i in currencies.indices) {
            if (currencies[i].endsWith(strCurrency)) {
                selected = i
                break
            }
        }

        AlertDialog.Builder(activity!!, R.style.AlertDialogStyle)
            .setTitle(R.string.select_currency)
            .setSingleChoiceItems(currencies, selected) { dialog, which ->
                val fiatUnit = currencies[which].substring(currencies[which].length - 3)
                settingsPresenter.updateFiatUnit(fiatUnit)
                dialog.dismiss()
            }
            .show()
    }

    override fun showDialogVerifySms() {
        val editText = AppCompatEditText(activity!!)
        editText.setSingleLine(true)

        val dialog = AlertDialog.Builder(activity!!, R.style.AlertDialogStyle)
            .setTitle(R.string.verify_mobile)
            .setMessage(R.string.verify_sms_summary)
            .setView(ViewUtils.getAlertDialogPaddedView(activity, editText))
            .setCancelable(false)
            .setPositiveButton(R.string.verify, null)
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.resend) { dialogInterface, i ->
                settingsPresenter.updateSms(
                    settingsPresenter.sms
                )
            }
            .create()

        dialog.setOnShowListener {
            val positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positive.setOnClickListener {
                val codeS = editText.text.toString()
                if (codeS.isNotEmpty()) {
                    settingsPresenter.verifySms(codeS)
                    dialog.dismiss()
                    ViewUtils.hideKeyboard(activity!!)
                }
            }
        }

        dialog.show()
    }

    private fun showDialogChangePin() {
        val intent = Intent(activity, PinEntryActivity::class.java)
        intent.putExtra(KEY_VALIDATING_PIN_FOR_RESULT, true)
        startActivityForResult(intent, REQUEST_CODE_VALIDATE_PIN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK)
            if (requestCode == REQUEST_CODE_VALIDATE_PIN) {
                settingsPresenter.pinCodeValidatedForChange()
            } else if (requestCode == CardDetailsActivity.ADD_CARD_REQUEST_CODE) {
                updateCards(listOf(
                    (data?.extras?.getSerializable(CardDetailsActivity.CARD_KEY) as? PaymentMethod.Card) ?: return
                ))
            }
    }

    private fun showDialogEmailNotifications() {
        val dialog = AlertDialog.Builder(activity!!, R.style.AlertDialogStyle)
            .setTitle(R.string.email_notifications)
            .setMessage(R.string.email_notifications_summary)
            .setPositiveButton(R.string.enable) { dialogInterface, i ->
                settingsPresenter.updateNotification(
                    Settings.NOTIFICATION_TYPE_EMAIL,
                    true
                )
            }
            .setNegativeButton(R.string.disable) { _, _ ->
                settingsPresenter.updateNotification(
                    Settings.NOTIFICATION_TYPE_EMAIL,
                    false
                )
            }
            .create()

        dialog.setOnCancelListener {
            emailNotificationPref.isChecked = !emailNotificationPref.isChecked
        }
        dialog.show()
    }

    private fun showDialogPushNotifications() {
        val dialog = AlertDialog.Builder(activity!!, R.style.AlertDialogStyle)
            .setTitle(R.string.push_notifications)
            .setMessage(R.string.push_notifications_summary)
            .setPositiveButton(R.string.enable) { _, _ -> settingsPresenter.enablePushNotifications() }
            .setNegativeButton(R.string.disable) { _, _ -> settingsPresenter.disablePushNotifications() }
            .create()

        dialog.setOnCancelListener {
            pushNotificationPref.isChecked = !pushNotificationPref.isChecked
        }
        dialog.show()
    }

    private fun showDialogChangePasswordWarning() {
        AlertDialog.Builder(activity!!, R.style.AlertDialogStyle)
            .setTitle(R.string.warning)
            .setMessage(R.string.change_password_summary)
            .setPositiveButton(R.string.dialog_continue) { _, _ -> showDialogChangePassword() }
            .show()
    }

    private fun showDialogChangePassword() {
        val inflater = activity!!.layoutInflater
        val pwLayout = inflater.inflate(R.layout.modal_change_password2, null) as LinearLayout

        val currentPassword = pwLayout.findViewById<AppCompatEditText>(R.id.current_password)
        val newPassword = pwLayout.findViewById<AppCompatEditText>(R.id.new_password)
        val newPasswordConfirmation =
            pwLayout.findViewById<AppCompatEditText>(R.id.confirm_password)

        val entropyMeter = pwLayout.findViewById<RelativeLayout>(R.id.entropy_meter)
        val passStrengthBar = pwLayout.findViewById<ProgressBar>(R.id.pass_strength_bar)
        passStrengthBar.max = 100
        val passStrengthVerdict = pwLayout.findViewById<TextView>(R.id.pass_strength_verdict)

        newPassword.addTextChangedListener(object : AfterTextChangedWatcher() {
            override fun afterTextChanged(editable: Editable) {
                newPassword.postDelayed({
                    if (activity != null && !activity!!.isFinishing) {
                        entropyMeter.visibility = View.VISIBLE
                        setPasswordStrength(
                            passStrengthVerdict,
                            passStrengthBar,
                            editable.toString()
                        )
                    }
                }, 200)
            }
        })

        val alertDialog = AlertDialog.Builder(activity!!, R.style.AlertDialogStyle)
            .setTitle(R.string.change_password)
            .setCancelable(false)
            .setView(pwLayout)
            .setPositiveButton(R.string.update, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        alertDialog.setOnShowListener {
            val buttonPositive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            buttonPositive.setOnClickListener {

                val currentPw = currentPassword.text.toString()
                val newPw = newPassword.text.toString()
                val newConfirmedPw = newPasswordConfirmation.text.toString()
                val walletPassword = settingsPresenter.tempPassword

                if (currentPw != newPw) {
                    if (currentPw == walletPassword) {
                        if (newPw == newConfirmedPw) {
                            if (newConfirmedPw.length < 4 || newConfirmedPw.length > 255) {
                                ToastCustom.makeText(
                                    activity,
                                    getString(R.string.invalid_password),
                                    ToastCustom.LENGTH_SHORT,
                                    ToastCustom.TYPE_ERROR
                                )
                            } else if (pwStrength < 50) {
                                AlertDialog.Builder(activity!!, R.style.AlertDialogStyle)
                                    .setTitle(R.string.app_name)
                                    .setMessage(R.string.weak_password)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.common_yes) { _, _ ->
                                        newPasswordConfirmation.setText("")
                                        newPasswordConfirmation.requestFocus()
                                        newPassword.setText("")
                                        newPassword.requestFocus()
                                    }
                                    .setNegativeButton(R.string.polite_no) { _, _ ->
                                        alertDialog.dismiss()
                                        settingsPresenter.updatePassword(
                                            newConfirmedPw,
                                            walletPassword
                                        )
                                    }
                                    .show()
                            } else {
                                alertDialog.dismiss()
                                settingsPresenter.updatePassword(newConfirmedPw, walletPassword)
                            }
                        } else {
                            newPasswordConfirmation.setText("")
                            newPasswordConfirmation.requestFocus()
                            showCustomToast(R.string.password_mismatch_error)
                        }
                    } else {
                        currentPassword.setText("")
                        currentPassword.requestFocus()
                        showCustomToast(R.string.invalid_password)
                    }
                } else {
                    newPassword.setText("")
                    newPasswordConfirmation.setText("")
                    newPassword.requestFocus()
                    showCustomToast(R.string.change_password_new_matches_current)
                }
            }
        }
        alertDialog.show()
    }

    private fun showCustomToast(@StringRes stringId: Int) {
        ToastCustom.makeText(
            activity,
            getString(stringId),
            ToastCustom.LENGTH_LONG,
            ToastCustom.TYPE_ERROR
        )
    }

    private fun showDialogTwoFA() {
        if (settingsPresenter.authType == Settings.AUTH_TYPE_GOOGLE_AUTHENTICATOR ||
            settingsPresenter.authType == Settings.AUTH_TYPE_YUBI_KEY
        ) {
            twoStepVerificationPref?.isChecked = true
            AlertDialog.Builder(activity!!, R.style.AlertDialogStyle)
                .setTitle(R.string.warning)
                .setCancelable(false)
                .setMessage(R.string.disable_online_only)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show()
        } else if (!settingsPresenter.isSmsVerified) {
            twoStepVerificationPref?.isChecked = false
            showDialogMobile()
        } else {
            val message = Html.fromHtml(getString(R.string.two_fa_description, URL_LOGIN))
            val spannable = SpannableString(message)
            Linkify.addLinks(spannable, Linkify.WEB_URLS)

            val alertDialogBuilder = AlertDialog.Builder(activity!!, R.style.AlertDialogStyle)
                .setTitle(R.string.two_fa)
                .setCancelable(false)
                .setMessage(spannable)
                .setNeutralButton(android.R.string.cancel) { _, _ ->
                    twoStepVerificationPref?.isChecked =
                        settingsPresenter.authType != Settings.AUTH_TYPE_OFF
                }

            if (settingsPresenter.authType != Settings.AUTH_TYPE_OFF) {
                alertDialogBuilder.setNegativeButton(R.string.disable) { _, _ ->
                    settingsPresenter.updateTwoFa(
                        Settings.AUTH_TYPE_OFF
                    )
                }
            } else {
                alertDialogBuilder.setPositiveButton(R.string.enable) { _, _ ->
                    settingsPresenter.updateTwoFa(
                        Settings.AUTH_TYPE_SMS
                    )
                }
            }
            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()
            (alertDialog.findViewById<View>(android.R.id.message) as TextView).movementMethod =
                LinkMovementMethod.getInstance()
        }
    }

    private fun setPasswordStrength(
        passStrengthVerdict: TextView,
        passStrengthBar: ProgressBar,
        pw: String
    ) {
        if (activity != null && !activity!!.isFinishing) {
            val strengthVerdicts = intArrayOf(
                R.string.strength_weak,
                R.string.strength_medium,
                R.string.strength_normal,
                R.string.strength_strong
            )
            val strengthColors = intArrayOf(
                R.drawable.progress_red,
                R.drawable.progress_orange,
                R.drawable.progress_blue,
                R.drawable.progress_green
            )
            pwStrength = Math.round(PasswordUtil.getStrength(pw)).toInt()

            if (pw == settingsPresenter.email) pwStrength = 0

            // red
            var pwStrengthLevel = 0

            when {
                pwStrength >= 75 -> // green
                    pwStrengthLevel = 3
                pwStrength >= 50 -> // green
                    pwStrengthLevel = 2
                pwStrength >= 25 -> // orange
                    pwStrengthLevel = 1
            }

            passStrengthBar.progress = pwStrength
            passStrengthBar.progressDrawable =
                ContextCompat.getDrawable(activity!!, strengthColors[pwStrengthLevel])
            passStrengthVerdict.text = resources.getString(strengthVerdicts[pwStrengthLevel])
        }
    }

    override fun launchKycFlow() {
        KycNavHostActivity.start(requireContext(), CampaignType.Swap, true)
        requireActivity().finish()
    }

    private fun setCountryFlag(tvCountry: TextView, dialCode: String, flagResourceId: Int) {
        tvCountry.text = dialCode
        val drawable = ContextCompat.getDrawable(activity!!, flagResourceId)
        drawable!!.alpha = 30
        tvCountry.background = drawable
    }

    override fun onDestroy() {
        super.onDestroy()
        hideProgressDialog()
        settingsPresenter.onViewDestroyed()
    }

    companion object {
        const val URL_LOGIN = "<a href=\"https://login.blockchain.com/\">login.blockchain.com</a>"

        internal const val EXTRA_SHOW_ADD_EMAIL_DIALOG = "show_add_email_dialog"
        internal const val EXTRA_SHOW_TWO_FA_DIALOG = "show_two_fa_dialog"
        private const val ADD_CARD_KEY = "ADD_CARD_KEY"
        private const val LINK_BANK_KEY = "ADD_BANK_KEY"
    }

    override fun onCardRemoved(cardId: String) {
        cardsPref?.findPreference<CardPreference>(cardId)?.let {
            cardsPref?.removePreference(it)
        }
    }

    override fun onLinkedBankRemoved(bankId: String) {
        banksPref?.findPreference<BankPreference>(bankId)?.let {
            banksPref?.removePreference(it)
        }
    }

    override fun onSheetClosed() {}

    override fun cardsEnabled(enabled: Boolean) {
        cardsPref?.isVisible = enabled
    }

    override fun banksEnabled(enabled: Boolean) {
        banksPref?.isVisible = enabled
    }
}

fun Preference?.onClick(onClick: () -> Unit) {
    this?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
        onClick()
        true
    }
}