package piuk.blockchain.android.ui.start

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting

import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.HDWalletException
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.MvpPresenter
import piuk.blockchain.android.ui.base.MvpView
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.android.util.AppUtil
import retrofit2.Response
import timber.log.Timber

interface ManualPairingView : MvpView {
    val guid: String
    val password: String
    fun goToPinPage()
    fun showToast(@StringRes messageId: Int, @ToastCustom.ToastType toastType: String)
    fun updateWaitingForAuthDialog(secondsRemaining: Int)
    fun resetPasswordField()
    fun showTwoFactorCodeNeededDialog(
        responseObject: JSONObject,
        sessionId: String,
        authType: Int,
        guid: String,
        password: String
    )
}

class ManualPairingPresenter(
    internal val appUtil: AppUtil,
    private val authDataManager: AuthDataManager,
    private val payloadDataManager: PayloadDataManager,
    private val prefs: PersistentPrefs,
    private val analytics: Analytics
) : MvpPresenter<ManualPairingView>() {

    private val authCompositeDispossable = CompositeDisposable()

    override fun onViewAttached() { /* no-op */
    }

    override fun onViewDetached() { /* no-op */
    }

    override val alwaysDisableScreenshots = true
    override val enableLogoutTimer = false

    private var sessionId: String? = null

    @VisibleForTesting
    internal var waitingForAuth = false

    internal fun onContinueClicked() {
        // Seems that on low memory devices it's quite possible that the view is null here
        view?.let {
            val guid = it.guid
            val password = it.password

            when {
                guid.isEmpty() -> showErrorToast(R.string.invalid_guid)
                password.isEmpty() -> showErrorToast(R.string.invalid_password)
                else -> verifyPassword(password, guid)
            }
        }
    }

    internal fun submitTwoFactorCode(
        responseObject: JSONObject,
        sessionId: String,
        guid: String,
        password: String,
        code: String?
    ) {
        if (code.isNullOrEmpty()) {
            view?.showToast(R.string.two_factor_null_error, ToastCustom.TYPE_ERROR)
        } else {
            compositeDisposable += authDataManager.submitTwoFactorCode(sessionId, guid, code)
                .doOnSubscribe {
                    view?.showProgressDialog(R.string.please_wait)
                }
                .doAfterTerminate { view?.dismissProgressDialog() }
                .subscribe(
                    { response ->
                        // This is slightly hacky, but if the user requires 2FA login,
                        // the payload comes in two parts. Here we combine them and
                        // parse/decrypt normally.
                        responseObject.put("payload", response.string())
                        val responseBody = responseObject.toString()
                            .toResponseBody("application/json".toMediaTypeOrNull())

                        val payload = Response.success(responseBody)
                        handleResponse(password, guid, payload)
                    },
                    {
                        showErrorToast(R.string.two_factor_incorrect_error)
                    }
                )
        }
    }

    private fun getSessionId(guid: String): Observable<String> =
        sessionId?.let { Observable.just(it) } ?: authDataManager.getSessionId(guid)

    private fun verifyPassword(password: String, guid: String) {
        waitingForAuth = true

        compositeDisposable += getSessionId(guid)
            .doOnSubscribe {
                view?.showProgressDialog(R.string.validating_password)
            }
            .doOnNext { s -> sessionId = s }
            .flatMap { sessionId -> authDataManager.getEncryptedPayload(guid, sessionId) }
            .subscribe(
                { response -> handleResponse(password, guid, response) },
                { throwable ->
                    Timber.e(throwable)
                    sessionId = null
                    showErrorToast(R.string.auth_failed)
                }
            )
    }

    private fun handleResponse(password: String, guid: String, response: Response<ResponseBody>) {
        val errorBody = if (response.errorBody() != null) response.errorBody()!!.string() else ""
        if (errorBody.contains(KEY_AUTH_REQUIRED)) {
            // 2FA
            showCheckEmailDialog()

            authCompositeDispossable += authDataManager.startPollingAuthStatus(guid, sessionId!!)
                .subscribe({ payloadResponse ->
                    waitingForAuth = false

                    if (payloadResponse == null || payloadResponse.contains(KEY_AUTH_REQUIRED)) {
                        showErrorToast(R.string.auth_failed)
                    } else {
                        val responseBody = payloadResponse.toResponseBody("application/json".toMediaTypeOrNull())
                        checkTwoFactor(password, guid, Response.success(responseBody))
                    }
                },
                    {
                        waitingForAuth = false
                        showErrorToast(R.string.auth_failed)
                    })
        } else {
            // No 2FA
            waitingForAuth = false
            checkTwoFactor(password, guid, response)
        }
    }

    private fun checkTwoFactor(password: String, guid: String, response: Response<ResponseBody>) {

        val responseBody = response.body()!!.string()
        val jsonObject = JSONObject(responseBody)
        // Check if the response has a 2FA Auth Type but is also missing the payload,
        // as it comes in two parts if 2FA enabled.
        if (jsonObject.isAuth() && (jsonObject.isGoogleAuth() || jsonObject.isSMSAuth())) {
            view?.dismissProgressDialog()
            view?.showTwoFactorCodeNeededDialog(
                jsonObject,
                sessionId!!,
                jsonObject.getInt("auth_type"),
                guid,
                password
            )
        } else {
            attemptDecryptPayload(password, responseBody)
        }
    }

    private fun attemptDecryptPayload(password: String, payload: String) {
        compositeDisposable += payloadDataManager.initializeFromPayload(payload, password)
            .doOnComplete {
                appUtil.sharedKey = payloadDataManager.wallet!!.sharedKey
                prefs.setValue(PersistentPrefs.KEY_WALLET_GUID, payloadDataManager.wallet!!.guid)
                prefs.setValue(PersistentPrefs.KEY_EMAIL_VERIFIED, true)
            }
            .subscribe(
                {
                    view?.goToPinPage()
                    analytics.logEvent(AnalyticsEvents.WalletManualLogin)
                },
                { throwable ->
                    when (throwable) {
                        is HDWalletException -> showErrorToast(R.string.pairing_failed)
                        is DecryptionException -> showErrorToast(R.string.invalid_password)
                        else -> showErrorToastAndRestartApp(R.string.auth_failed)
                    }
                }
            )
    }

    private fun showCheckEmailDialog() {
        authCompositeDispossable += authDataManager.createCheckEmailTimer()
            .doOnSubscribe {
                view?.showProgressDialog(R.string.check_email_to_auth_login, ::onProgressCancelled)
            }
            .takeUntil { !waitingForAuth }
            .subscribe({ integer ->
                if (integer <= 0) {
                    // Only called if timer has run out
                    showErrorToastAndRestartApp(R.string.pairing_failed)
                } else {
                    view?.updateWaitingForAuthDialog(integer!!)
                }
            },
                {
                    showErrorToast(R.string.auth_failed)
                    waitingForAuth = false
                })
    }

    internal fun onProgressCancelled() {
        waitingForAuth = false
        compositeDisposable.clear()
    }

    private fun showErrorToast(@StringRes message: Int) {
        view?.apply {
            dismissProgressDialog()
            resetPasswordField()
            showToast(message, ToastCustom.TYPE_ERROR)
        }
    }

    private fun showErrorToastAndRestartApp(@StringRes message: Int) {
        view?.apply {
            resetPasswordField()
            dismissProgressDialog()
            showToast(message, ToastCustom.TYPE_ERROR)
        }
        appUtil.clearCredentialsAndRestart(LauncherActivity::class.java)
    }

    companion object {
        @VisibleForTesting
        internal val KEY_AUTH_REQUIRED = "authorization_required"
    }

    fun onViewDestroyed() {
        authCompositeDispossable.clear()
    }
}

private fun JSONObject.isAuth(): Boolean =
    has("auth_type") && !has("payload")

private fun JSONObject.isGoogleAuth(): Boolean =
    getInt("auth_type") == Settings.AUTH_TYPE_GOOGLE_AUTHENTICATOR

private fun JSONObject.isSMSAuth(): Boolean =
    getInt("auth_type") == Settings.AUTH_TYPE_SMS
