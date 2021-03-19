package piuk.blockchain.android.ui.start

import com.blockchain.nabu.datamanagers.ApiStatus
import com.blockchain.preferences.SecurityPrefs
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.ui.base.MvpPresenter
import piuk.blockchain.android.ui.base.MvpView
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.RootUtil
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import timber.log.Timber

interface LandingView : MvpView {
    fun showDebugMenu()
    fun showToast(message: String, @ToastCustom.ToastType toastType: String)
    fun showIsRootedWarning()
    fun showApiOutageMessage()
}

class LandingPresenter(
    private val environmentSettings: EnvironmentConfig,
    private val prefs: SecurityPrefs,
    private val rootUtil: RootUtil,
    private val apiStatus: ApiStatus
) : MvpPresenter<LandingView>() {

    override val alwaysDisableScreenshots = false
    override val enableLogoutTimer = false

    override fun onViewAttached() {
        if (environmentSettings.isRunningInDebugMode()) {
            view?.let {
                it.showToast(
                    "Current environment: ${environmentSettings.environment.getName()}",
                    ToastCustom.TYPE_GENERAL
                )
                it.showDebugMenu()
            }
        }
        checkApiStatus()
    }

    private fun checkApiStatus() {
        compositeDisposable += apiStatus.isHealthy()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = { isHealthy ->
                if (isHealthy.not())
                    view?.showApiOutageMessage()
            }, onError = {
                Timber.e(it)
            })
    }

    override fun onViewDetached() { /* no-op */ }

    internal fun checkForRooted() {
        if (rootUtil.isDeviceRooted && !prefs.disableRootedWarning) {
            view?.showIsRootedWarning()
        }
    }
}