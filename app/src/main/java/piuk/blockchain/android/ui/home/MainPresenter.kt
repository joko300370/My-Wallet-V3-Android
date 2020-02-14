package piuk.blockchain.android.ui.home

import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import com.blockchain.swap.nabu.models.nabu.CampaignData
import com.blockchain.swap.nabu.models.nabu.KycState
import com.blockchain.swap.nabu.models.nabu.NabuApiException
import com.blockchain.swap.nabu.models.nabu.NabuErrorCodes
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.ui.kyc.settings.KycStatusHelper
import piuk.blockchain.android.campaign.SunriverCampaignRegistration
import piuk.blockchain.android.campaign.SunriverCardType
import com.blockchain.lockbox.data.LockboxDataManager
import com.blockchain.logging.CrashLogger
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.swap.nabu.datamanagers.NabuDataManager
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.deeplink.DeepLinkProcessor
import piuk.blockchain.android.deeplink.EmailVerifiedLinkState
import piuk.blockchain.android.deeplink.LinkState
import piuk.blockchain.android.kyc.KycLinkState
import piuk.blockchain.android.simplebuy.SimpleBuyAvailability
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.sunriver.CampaignLinkState
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.android.ui.base.MvpPresenter
import piuk.blockchain.android.ui.base.MvpView
import piuk.blockchain.androidbuysell.models.WebViewLoginDetails
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcoreui.utils.logging.Logging
import piuk.blockchain.androidcoreui.utils.logging.SecondPasswordEvent
import timber.log.Timber

interface MainView : MvpView, HomeNavigator {

    @Deprecated("Used for processing deep links. Find a way to get rid of this")
    fun getStartIntent(): Intent

    fun onHandleInput(strUri: String)
    fun startBalanceFragment()
    fun kickToLauncherPage()
    fun showProgressDialog(@StringRes message: Int)
    fun hideProgressDialog()
    fun clearAllDynamicShortcuts()
    fun showMetadataNodeFailure()
    fun setBuySellEnabled(enabled: Boolean, useWebView: Boolean)
    fun setPitEnabled(enabled: Boolean)
    fun setSimpleBuyEnabled(enabled: Boolean)
    fun showTradeCompleteMsg(txHash: String)
    fun setWebViewLoginDetails(loginDetails: WebViewLoginDetails)
    fun showSecondPasswordDialog()
    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)
    fun showHomebrewDebugMenu()
    fun enableSwapButton(isEnabled: Boolean)
    fun displayLockboxMenu(lockboxAvailable: Boolean)
    fun showTestnetWarning()
    fun launchSwapIntro()
    fun launchPendingVerificationScreen(campaignType: CampaignType)
    fun shouldIgnoreDeepLinking(): Boolean
    fun displayDialog(@StringRes title: Int, @StringRes message: Int)
}

class MainPresenter internal constructor(
    private val prefs: PersistentPrefs,
    private val appUtil: AppUtil,
    private val accessState: AccessState,
    private val metadataLoader: MetadataLoader,
    private val payloadDataManager: PayloadDataManager,
    private val buyDataManager: BuyDataManager,
    private val exchangeRateFactory: ExchangeRateDataManager,
    private val currencyState: CurrencyState,
    private val metadataManager: MetadataManager,
    private val environmentSettings: EnvironmentConfig,
    private val coinifyDataManager: CoinifyDataManager,
    private val exchangeService: ExchangeService,
    private val kycStatusHelper: KycStatusHelper,
    private val lockboxDataManager: LockboxDataManager,
    private val deepLinkProcessor: DeepLinkProcessor,
    private val sunriverCampaignRegistration: SunriverCampaignRegistration,
    private val xlmDataManager: XlmDataManager,
    private val pitFeatureFlag: FeatureFlag,
    private val pitLinking: PitLinking,
    private val nabuDataManager: NabuDataManager,
    private val simpleBuySync: SimpleBuySyncFactory,
    private val crashLogger: CrashLogger,
    private val simpleBuyAvailability: SimpleBuyAvailability,
    nabuToken: NabuToken
) : MvpPresenter<MainView>() {

    override val alwaysDisableScreenshots: Boolean = false
    override val enableLogoutTimer: Boolean = true

    internal val defaultCurrency: String
        get() = prefs.selectedFiatCurrency

    private val nabuUser = nabuToken
        .fetchNabuToken()
        .flatMap {
            nabuDataManager.getUser(it)
        }

    override fun onViewAttached() {
        if (!accessState.isLoggedIn) {
            // This should never happen, but handle the scenario anyway by starting the launcher
            // activity, which handles all login/auth/corruption scenarios itself
            view?.kickToLauncherPage()
        } else {
            logEvents()

            checkLockboxAvailability()

            initMetadataElements()

            doPushNotifications()

            checkPitAvailability()
        }
    }

    private fun initSimpleBuyState() {
        compositeDisposable +=
            simpleBuyAvailability.isAvailable()
                .doOnSubscribe { view?.setSimpleBuyEnabled(false) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        view?.setSimpleBuyEnabled(it)
                        if (it) {
                            view?.setBuySellEnabled(enabled = false, useWebView = false)
                        }
                    }
                )
    }

    override fun onViewDetached() {}

    private fun checkPitAvailability() {
        compositeDisposable += pitFeatureFlag.enabled.subscribeBy { view?.setPitEnabled(it) }
    }

    private fun checkLockboxAvailability() {
        compositeDisposable += lockboxDataManager.isLockboxAvailable()
            .subscribe { enabled, _ -> view?.displayLockboxMenu(enabled) }
    }

    /**
     * Initial setup of push notifications. We don't subscribe to addresses for notifications when
     * creating a new wallet. To accommodate existing wallets we need subscribe to the next
     * available addresses.
     */
    private fun doPushNotifications() {
        if (prefs.arePushNotificationsEnabled) {
            compositeDisposable += payloadDataManager.syncPayloadAndPublicKeys()
                .subscribe({ /*no-op*/ },
                    { throwable -> Timber.e(throwable) })
        }
    }

    internal fun doTestnetCheck() {
        if (environmentSettings.environment == Environment.TESTNET) {
            currencyState.cryptoCurrency = CryptoCurrency.BTC
            view?.showTestnetWarning()
        }
    }

    private fun checkKycStatus() {
        compositeDisposable += kycStatusHelper.shouldDisplayKyc()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { view?.enableSwapButton(it) },
                { Timber.e(it) }
            )
    }

    private fun setDebugExchangeVisibility() {
        if (BuildConfig.DEBUG) {
            view?.showHomebrewDebugMenu()
        }
    }

    internal fun initMetadataElements() {
        compositeDisposable += metadataLoader.loader()
            .flatMapCompletable { firstLoad ->
                if (firstLoad) {
                    simpleBuySync.performSync()
                } else {
                    Completable.complete()
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                view?.showProgressDialog(R.string.please_wait)
            }
            .doAfterTerminate {
                view?.hideProgressDialog()

                val strUri = prefs.getValue(PersistentPrefs.KEY_SCHEME_URL, "")
                if (strUri.isNotEmpty()) {
                    prefs.removeValue(PersistentPrefs.KEY_SCHEME_URL)
                    view?.onHandleInput(strUri)
                }
            }
            .subscribeBy(
                onComplete = {
                    checkKycStatus()
                    setDebugExchangeVisibility()
                    initBuyService()
                    initSimpleBuyState()
                    checkForPendingLinks()
                },
                onError = { throwable ->
                    if (throwable is InvalidCredentialsException || throwable is HDWalletException) {
                        if (payloadDataManager.isDoubleEncrypted) {
                            // Wallet double encrypted and needs to be decrypted to set up ether wallet, contacts etc
                            view?.showSecondPasswordDialog()
                        } else {
                            logException(throwable)
                        }
                    } else {
                        logException(throwable)
                    }
                }
            )
    }

    fun handlePossibleDeepLink(url: String) {
        try {
            val link = Uri.parse(url).getQueryParameter("link") ?: return
            compositeDisposable += deepLinkProcessor.getLink(link)
                .subscribeBy(
                    onError = { Timber.e(it) },
                    onSuccess = { dispatchDeepLink(it) }
                )
        } catch (t: Throwable) {
            Timber.d("Invalid link cannot be processed - ignoring")
        }
    }

    private fun checkForPendingLinks() {
        compositeDisposable += deepLinkProcessor.getLink(view!!.getStartIntent())
            .filter { !view!!.shouldIgnoreDeepLinking() }
            .subscribeBy(
                onError = { Timber.e(it) },
                onSuccess = { dispatchDeepLink(it) }
            )
    }

    private fun dispatchDeepLink(linkState: LinkState) {
        when (linkState) {
            is LinkState.SunriverDeepLink -> handleSunriverDeepLink(linkState)
            is LinkState.EmailVerifiedDeepLink -> handleEmailVerifiedDeepLink(linkState)
            is LinkState.KycDeepLink -> handleKycDeepLink(linkState)
            is LinkState.ThePitDeepLink -> handleThePitDeepLink(linkState)
        }
    }

    private fun handleSunriverDeepLink(linkState: LinkState.SunriverDeepLink) {
        when (linkState.link) {
            is CampaignLinkState.WrongUri -> view?.displayDialog(
                R.string.sunriver_invalid_url_title,
                R.string.sunriver_invalid_url_message
            )
            is CampaignLinkState.Data -> registerForCampaign(linkState.link.campaignData)
        }
    }

    private fun handleKycDeepLink(linkState: LinkState.KycDeepLink) {
        when (linkState.link) {
            is KycLinkState.Resubmit -> view?.launchKyc(CampaignType.Resubmission)
            is KycLinkState.EmailVerified -> view?.launchKyc(CampaignType.Swap)
            is KycLinkState.General -> {
                val data = linkState.link.campaignData
                if (data != null) {
                    registerForCampaign(data)
                } else {
                    view?.launchKyc(CampaignType.Swap)
                }
            }
        }
    }

    private fun handleThePitDeepLink(linkState: LinkState.ThePitDeepLink) {
        view?.launchThePitLinking(linkState.linkId)
    }

    private fun handleEmailVerifiedDeepLink(linkState: LinkState.EmailVerifiedDeepLink) {
        if (linkState.link === EmailVerifiedLinkState.FromPitLinking) {
            showThePitOrPitLinkingView(prefs.pitToWalletLinkId)
        }
    }

    private fun registerForCampaign(data: CampaignData) {
        compositeDisposable +=
            xlmDataManager.defaultAccount()
                .flatMapCompletable {
                    sunriverCampaignRegistration
                        .registerCampaign(data)
                }
                .andThen(kycStatusHelper.getKycStatus())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view?.showProgressDialog(R.string.please_wait) }
                .doOnEvent { _, _ -> view?.hideProgressDialog() }
                .subscribe({ status ->
                    prefs.setValue(SunriverCardType.JoinWaitList.javaClass.simpleName, true)
                    if (status != KycState.Verified) {
                        view?.launchKyc(CampaignType.Sunriver)
                    }
                }, { throwable ->
                    Timber.e(throwable)
                    if (throwable is NabuApiException) {
                        val errorMessageStringId =
                            when (val errorCode = throwable.getErrorCode()) {
                                NabuErrorCodes.InvalidCampaignUser ->
                                    R.string.sunriver_invalid_campaign_user
                                NabuErrorCodes.CampaignUserAlreadyRegistered ->
                                    R.string.sunriver_user_already_registered
                                NabuErrorCodes.CampaignExpired ->
                                    R.string.sunriver_campaign_expired
                                else -> {
                                    Timber.e("Unknown server error $errorCode ${errorCode.code}")
                                    R.string.sunriver_generic_error
                                }
                            }
                        view?.displayDialog(
                            R.string.sunriver_invalid_url_title,
                            errorMessageStringId
                        )
                    }
                }
                )
    }

    private fun logException(throwable: Throwable) {
        crashLogger.logException(throwable)
        view?.showMetadataNodeFailure()
    }

    internal fun unPair() {
        view?.clearAllDynamicShortcuts()
        metadataLoader.unload()
    }

    internal fun updateTicker() {
        compositeDisposable +=
            exchangeRateFactory.updateTickers()
                .subscribeBy(onError = { it.printStackTrace() }, onComplete = {})
    }

    private fun logEvents() {
        Logging.logCustom(SecondPasswordEvent(payloadDataManager.isDoubleEncrypted))
    }

    private fun initBuyService() {
        compositeDisposable +=
            Singles.zip(buyDataManager.canBuy,
                simpleBuyAvailability.isAvailable(),
                buyDataManager.isCoinifyAllowed).observeOn(AndroidSchedulers.mainThread()).subscribe(
                { (isEnabled, available, isCoinifyAllowed) ->
                    view?.setBuySellEnabled(isEnabled && !available, isCoinifyAllowed)
                    if (isEnabled && !isCoinifyAllowed) {
                        compositeDisposable += buyDataManager.watchPendingTrades()
                            .applySchedulers()
                            .subscribe({ view?.showTradeCompleteMsg(it) }, { it.printStackTrace() })

                        compositeDisposable += buyDataManager.webViewLoginDetails
                            .subscribe({ view?.setWebViewLoginDetails(it) }, { it.printStackTrace() })
                    } else if (isEnabled && isCoinifyAllowed && !available) {
                        notifyCompletedCoinifyTrades()
                    }
                }, { throwable ->
                    Timber.e(throwable)
                    view?.setBuySellEnabled(enabled = false, useWebView = false)
                })
    }

    private fun notifyCompletedCoinifyTrades() {
        compositeDisposable +=
            CoinifyTradeCompleteListener(exchangeService, coinifyDataManager, metadataManager)
                .getCompletedCoinifyTradesAndUpdateMetaData()
                .firstElement()
                .applySchedulers()
                .subscribeBy({
                    Timber.e(it)
                }) { view?.showTradeCompleteMsg(it) }
    }

    internal fun decryptAndSetupMetadata(secondPassword: String) {
        if (!payloadDataManager.validateSecondPassword(secondPassword)) {
            view?.showToast(R.string.invalid_password, ToastCustom.TYPE_ERROR)
            view?.showSecondPasswordDialog()
        } else {
            compositeDisposable += metadataManager.decryptAndSetupMetadata(
                environmentSettings.bitcoinNetworkParameters,
                secondPassword
            ).subscribeBy(
                onError = { it.printStackTrace() },
                onComplete = { appUtil.restartApp(LauncherActivity::class.java) }
            )
        }
    }

    internal fun setCryptoCurrency(cryptoCurrency: CryptoCurrency) {
        currencyState.cryptoCurrency = cryptoCurrency
    }

    internal fun routeToBuySell() {
        compositeDisposable += buyDataManager.isCoinifyAllowed
            .subscribeBy(onError = { it.printStackTrace() },
                onSuccess = { coinifyAllowed ->
                    if (coinifyAllowed)
                        view?.launchBuySell()
                })
    }

    internal fun clearLoginState() {
        accessState.logout()
    }

    internal fun startSwapOrKyc(toCurrency: CryptoCurrency?, fromCurrency: CryptoCurrency?) {
        compositeDisposable += nabuUser
            .subscribeBy(onError = { it.printStackTrace() }, onSuccess = { nabuUser ->
                if (nabuUser.tiers?.current ?: 0 > 0) {
                    view?.launchSwap(
                        defCurrency = prefs.selectedFiatCurrency,
                        toCryptoCurrency = toCurrency,
                        fromCryptoCurrency = fromCurrency
                    )
                } else {
                    if (nabuUser.kycState == KycState.Rejected ||
                        nabuUser.kycState == KycState.UnderReview ||
                        prefs.swapIntroCompleted
                    )
                        view?.launchPendingVerificationScreen(CampaignType.Swap)
                    else
                        view?.launchSwapIntro()
                }
            })
    }

    fun onThePitMenuClicked() {
        showThePitOrPitLinkingView("")
    }

    private fun showThePitOrPitLinkingView(linkId: String) {
        compositeDisposable += pitLinking.isPitLinked().observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onError = { Timber.e(it) }, onSuccess = { isLinked ->
                if (isLinked) {
                    view?.launchThePit()
                } else {
                    view?.launchThePitLinking(linkId)
                }
            })
    }
}
