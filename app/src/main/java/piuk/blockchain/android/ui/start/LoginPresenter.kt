package piuk.blockchain.android.ui.start

import androidx.annotation.StringRes
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import io.reactivex.rxkotlin.plusAssign
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.MvpPresenter
import piuk.blockchain.android.ui.base.MvpView
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.android.util.AppUtil
import javax.net.ssl.SSLPeerUnverifiedException

interface LoginView : MvpView {
    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)
    fun startPinEntryActivity()
}

class LoginPresenter(
    private val appUtil: AppUtil,
    private val _payloadDataManager: Lazy<PayloadDataManager>,
    private val prefs: PersistentPrefs,
    private val analytics: Analytics
) : MvpPresenter<LoginView>() {

    override fun onViewAttached() { /* no-op */ }
    override fun onViewDetached() { /* no-op */ }

    override val alwaysDisableScreenshots: Boolean = false
    override val enableLogoutTimer: Boolean = false

    internal fun pairWithQR(raw: String?) {

        if (raw == null) view?.showToast(R.string.pairing_failed, ToastCustom.TYPE_ERROR)

        val dataManager = _payloadDataManager.value

        compositeDisposable += dataManager.handleQrCode(raw!!)
            .doOnSubscribe { view?.showProgressDialog(R.string.please_wait) }
            .doOnComplete { appUtil.sharedKey = dataManager.wallet!!.sharedKey }
            .doAfterTerminate { view?.dismissProgressDialog() }
            .subscribe({
                prefs.setValue(PersistentPrefs.KEY_WALLET_GUID, dataManager.wallet!!.guid)
                prefs.setValue(PersistentPrefs.KEY_EMAIL_VERIFIED, true)
                prefs.setValue(PersistentPrefs.KEY_ONBOARDING_COMPLETE, true)
                view?.startPinEntryActivity()

                analytics.logEvent(AnalyticsEvents.WalletAutoPairing)
            }, { throwable ->
                if (throwable is SSLPeerUnverifiedException) {
                    // BaseActivity handles message
                    appUtil.clearCredentials()
                } else {
                    view?.showToast(R.string.pairing_failed, ToastCustom.TYPE_ERROR)
                    appUtil.clearCredentialsAndRestart(LauncherActivity::class.java)
                }
            })
    }
}