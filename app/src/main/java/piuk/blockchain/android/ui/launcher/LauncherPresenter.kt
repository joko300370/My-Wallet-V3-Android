package piuk.blockchain.android.ui.launcher

import android.annotation.SuppressLint
import android.app.LauncherActivity
import android.content.Intent
import com.blockchain.notifications.NotificationTokenManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.api.data.Settings.UNIT_FIAT
import io.reactivex.Observable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.zipWith
import piuk.blockchain.android.R
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.android.util.AppUtil
import java.util.Locale
import java.util.Currency

class LauncherPresenter(
    private val appUtil: AppUtil,
    private val payloadDataManager: PayloadDataManager,
    private val prefs: PersistentPrefs,
    private val deepLinkPersistence: DeepLinkPersistence,
    private val accessState: AccessState,
    private val settingsDataManager: SettingsDataManager,
    private val notificationTokenManager: NotificationTokenManager,
    private val envSettings: EnvironmentConfig,
    private val featureFlag: FeatureFlag,
    private val currencyPrefs: CurrencyPrefs,
    private val custodialWalletManager: CustodialWalletManager
) : BasePresenter<LauncherView>() {

    override fun onViewReady() {
        val intent = view.getPageIntent()
        val action = intent.action
        val scheme = intent.scheme
        val intentData = intent.dataString
        val extras = intent.extras
        val hasLoggedOut = prefs.isLoggedOut
        var isPinValidated = false

        // Store incoming bitcoin URI if needed
        if (action != null && Intent.ACTION_VIEW == action && scheme != null && scheme == "bitcoin") {
            prefs.setValue(PersistentPrefs.KEY_SCHEME_URL, intent.data.toString())
        }

        if (Intent.ACTION_VIEW == action) {
            deepLinkPersistence.pushDeepLink(intent.data)
        }

        // Store incoming Contacts URI if needed
        if (action != null && Intent.ACTION_VIEW == action && intentData != null && intentData.contains(
                "blockchain"
            )
        ) {
            prefs.setValue(PersistentPrefs.KEY_METADATA_URI, intentData)
        }

        if (extras != null && extras.containsKey(INTENT_EXTRA_VERIFIED)) {
            isPinValidated = extras.getBoolean(INTENT_EXTRA_VERIFIED)
        }

        if (extras?.containsKey("IS_AUTOMATION_TESTING") == true) {
            if (extras.getBoolean(INTENT_AUTOMATION_TEST) && Environment.STAGING == envSettings.environment) {
                prefs.setIsUnderTest()
            }
        }

        when {
            // No GUID? Treat as new installation
            prefs.getValue(PersistentPrefs.KEY_WALLET_GUID, "").isEmpty() -> view.onNoGuid()
            // User has logged out recently. Show password reentry page
            hasLoggedOut -> view.onReEnterPassword()
            // No PIN ID? Treat as installed app without confirmed PIN
            prefs.getValue(PersistentPrefs.KEY_PIN_IDENTIFIER, "").isEmpty() -> view.onRequestPin()
            // Installed app, check sanity
            !appUtil.isSane -> view.onCorruptPayload()
            // Legacy app has not been prompted for upgrade
            isPinValidated && !payloadDataManager.wallet!!.isUpgraded -> promptUpgrade()
            // App has been PIN validated
            isPinValidated || accessState.isLoggedIn -> initSettings()
            // Something odd has happened, re-request PIN
            else -> view.onRequestPin()
        }
    }

    fun clearCredentialsAndRestart() =
        appUtil.clearCredentialsAndRestart(LauncherActivity::class.java)

    private fun promptUpgrade() {
        accessState.isLoggedIn = true
        view.onRequestUpgrade()
    }

    /**
     * Init of the [SettingsDataManager] must complete here so that we can access the [Settings]
     * object from memory when the user is logged in.
     */
    @SuppressLint("CheckResult")
    private fun initSettings() {
        compositeDisposable += settingsDataManager.initSettings(
            payloadDataManager.wallet!!.guid,
            payloadDataManager.wallet!!.sharedKey
        ).flatMap { settings ->
            if (!accessState.isNewlyCreated ||
                !UNIT_FIAT.contains(Currency.getInstance(Locale.getDefault()).currencyCode)
            ) {
                Observable.just(settings)
            } else {
                settingsDataManager.updateFiatUnit(fiatUnitForFreshAccount())
            }
        }.flatMapSingle { settings ->
            custodialWalletManager.isCurrencySupportedForSimpleBuy(settings.currency).zipWith(featureFlag.enabled)
                .map { (currencySupported, simpleBuyFeatureFlagEnabled) ->
                    val simpleBuyFlowCanBeLaunched =
                        currencySupported && simpleBuyFeatureFlagEnabled && accessState.isNewlyCreated
                    settings to simpleBuyFlowCanBeLaunched
                }
        }
            .doOnComplete { accessState.isLoggedIn = true }
            .doOnNext { notificationTokenManager.registerAuthEvent() }
            .subscribe(
                { (settings, simpleBuyEnabled) ->
                    setCurrencyUnits(settings)
                    if (simpleBuyEnabled &&
                        view?.getPageIntent()?.getBooleanExtra(AppUtil.INTENT_EXTRA_IS_AFTER_WALLET_CREATION,
                            false) == true
                    ) {
                        startSimpleBuy()
                    } else
                        startMainActivity()
                }, {
                    view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
                    view.onRequestPin()
                })
    }

    private fun startMainActivity() {
        view.onStartMainActivity(deepLinkPersistence.popUriFromSharedPrefs())
    }

    private fun startSimpleBuy() {
        view.startSimpleBuy()
    }

    private fun fiatUnitForFreshAccount() =
        currencyPrefs.selectedFiatCurrency

    private fun setCurrencyUnits(settings: Settings) {
        prefs.selectedFiatCurrency = settings.currency
    }

    private fun Settings.isSimpleBuyAllowed(): Boolean =
        listOf("EUR", "GBP").contains(currency)

    companion object {
        const val INTENT_EXTRA_VERIFIED = "verified"
        const val INTENT_AUTOMATION_TEST = "IS_AUTOMATION_TESTING"
    }
}