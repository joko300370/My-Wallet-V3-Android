package piuk.blockchain.android.ui.createwallet

import android.annotation.SuppressLint
import android.app.LauncherActivity
import android.content.Intent
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.crashlytics.android.answers.SignUpEvent
import info.blockchain.wallet.util.FormatsUtil
import info.blockchain.wallet.util.PasswordUtil
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.recover.RecoverFundsActivity
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.PrngFixer
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcoreui.utils.logging.Logging
import piuk.blockchain.androidcoreui.utils.logging.RecoverWalletEvent
import timber.log.Timber

class CreateWalletPresenter(
    private val payloadDataManager: PayloadDataManager,
    private val prefs: PersistentPrefs,
    private val appUtil: AppUtil,
    private val accessState: AccessState,
    private val prngFixer: PrngFixer,
    private val analytics: Analytics
) : BasePresenter<CreateWalletView>() {

    var recoveryPhrase: String = ""
    var passwordStrength = 0

    override fun onViewReady() {
        // No-op
    }

    fun parseExtras(intent: Intent) {
        analytics.logEventOnce(AnalyticsEvents.WalletSignupClickCreate)
        val mnemonic = intent.getStringExtra(RecoverFundsActivity.RECOVERY_PHRASE)

        if (mnemonic != null) recoveryPhrase = mnemonic

        if (recoveryPhrase.isNotEmpty()) {
            view.setTitleText(R.string.recover_funds)
            view.setNextText(R.string.dialog_continue)
        } else {
            view.setTitleText(R.string.new_account_title)
            view.setNextText(R.string.new_account_cta_text)
        }
    }

    fun calculateEntropy(password: String) {
        passwordStrength = Math.round(PasswordUtil.getStrength(password)).toInt()
        view.setEntropyStrength(passwordStrength)

        when (passwordStrength) {
            in 0..25 -> view.setEntropyLevel(0)
            in 26..50 -> view.setEntropyLevel(1)
            in 51..75 -> view.setEntropyLevel(2)
            in 76..100 -> view.setEntropyLevel(3)
        }
    }

    fun validateCredentials(email: String, password1: String, password2: String) {
        when {
            !FormatsUtil.isValidEmailAddress(email) -> view.showToast(
                R.string.invalid_email,
                ToastCustom.TYPE_ERROR
            )
            password1.length < 4 -> view.showToast(
                R.string.invalid_password_too_short,
                ToastCustom.TYPE_ERROR
            )
            password1.length > 255 -> view.showToast(
                R.string.invalid_password,
                ToastCustom.TYPE_ERROR
            )
            password1 != password2 -> view.showToast(
                R.string.password_mismatch_error,
                ToastCustom.TYPE_ERROR
            )
            passwordStrength < 50 -> view.showWeakPasswordDialog(
                email,
                password1)
            else -> createOrRecoverWallet(email, password1)
        }
    }

    fun createOrRecoverWallet(email: String, password: String) {
        analytics.logEventOnce(AnalyticsEvents.WalletSignupCreated)
        when {
            !recoveryPhrase.isEmpty() -> recoverWallet(email, password)
            else -> createWallet(email, password)
        }
    }

    @SuppressLint("CheckResult")
    private fun createWallet(email: String, password: String) {
        prngFixer.applyPRNGFixes()

        payloadDataManager.createHdWallet(password, view.getDefaultAccountName(), email)
            .doOnNext {
                accessState.isNewlyCreated = true
                prefs.setValue(PersistentPrefs.KEY_WALLET_GUID, payloadDataManager.wallet!!.guid)
                appUtil.sharedKey = payloadDataManager.wallet!!.sharedKey
            }
            .addToCompositeDisposable(this)
            .doOnSubscribe { view.showProgressDialog(R.string.creating_wallet) }
            .doOnTerminate { view.dismissProgressDialog() }
            .subscribe(
                {
                    prefs.setValue(PersistentPrefs.KEY_EMAIL, email)
                    view.startPinEntryActivity()
                    Logging.logSignUp(
                        SignUpEvent().putSuccess(true)
                    )
                    analytics.logEvent(AnalyticsEvents.WalletCreation)
                },
                {
                    Timber.e(it)
                    view.showToast(R.string.hd_error, ToastCustom.TYPE_ERROR)
                    appUtil.clearCredentialsAndRestart(LauncherActivity::class.java)
                    Logging.logSignUp(
                        SignUpEvent().putSuccess(false)
                    )
                }
            )
    }

    @SuppressLint("CheckResult")
    private fun recoverWallet(email: String, password: String) {
        payloadDataManager.restoreHdWallet(
            recoveryPhrase,
            view.getDefaultAccountName(),
            email,
            password
        ).doOnNext {
            accessState.isNewlyCreated = true
            accessState.isRestored = true
            prefs.setValue(PersistentPrefs.KEY_WALLET_GUID, payloadDataManager.wallet!!.guid)
            appUtil.sharedKey = payloadDataManager.wallet!!.sharedKey
        }.addToCompositeDisposable(this)
            .doOnSubscribe { view.showProgressDialog(R.string.restoring_wallet) }
            .doOnTerminate { view.dismissProgressDialog() }
            .subscribe(
                {
                    prefs.setValue(PersistentPrefs.KEY_EMAIL, email)
                    prefs.setValue(PersistentPrefs.KEY_ONBOARDING_COMPLETE, true)
                    view.startPinEntryActivity()
                    Logging.logCustom(
                        RecoverWalletEvent().putSuccess(true)
                    )
                },
                {
                    Timber.e(it)
                    view.showToast(R.string.restore_failed, ToastCustom.TYPE_ERROR)
                    Logging.logCustom(
                        RecoverWalletEvent().putSuccess(false)
                    )
                }
            )
    }

    fun logEventEmailClicked() = analytics.logEventOnce(AnalyticsEvents.WalletSignupClickEmail)
    fun logEventPasswordOneClicked() = analytics.logEventOnce(AnalyticsEvents.WalletSignupClickPasswordFirst)
    fun logEventPasswordTwoClicked() = analytics.logEventOnce(AnalyticsEvents.WalletSignupClickPasswordSecond)
}
