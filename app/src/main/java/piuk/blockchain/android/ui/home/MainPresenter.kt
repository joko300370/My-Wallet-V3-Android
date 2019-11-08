package piuk.blockchain.android.ui.home

import android.net.Uri
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.CampaignData
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.kyc.models.nabu.NabuApiException
import com.blockchain.kyc.models.nabu.NabuErrorCodes
import piuk.blockchain.android.ui.kyc.settings.KycStatusHelper
import piuk.blockchain.android.ui.kyc.sunriver.SunriverCampaignHelper
import piuk.blockchain.android.ui.kyc.sunriver.SunriverCardType
import com.blockchain.lockbox.data.LockboxDataManager
import com.blockchain.logging.CrashLogger
import com.blockchain.remoteconfig.ABTestExperiment
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.sunriver.XlmDataManager
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import info.blockchain.wallet.payload.PayloadManagerWiper
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.deeplink.DeepLinkProcessor
import piuk.blockchain.android.deeplink.EmailVerifiedLinkState
import piuk.blockchain.android.deeplink.LinkState
import piuk.blockchain.android.kyc.KycLinkState
import piuk.blockchain.android.sunriver.CampaignLinkState
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.android.ui.dashboard.DashboardPresenter
import piuk.blockchain.android.ui.home.models.MetadataEvent
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import com.blockchain.swap.shapeshift.ShapeShiftDataManager
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.AppUtil
import piuk.blockchain.androidcoreui.utils.logging.Logging
import piuk.blockchain.androidcoreui.utils.logging.SecondPasswordEvent
import timber.log.Timber

class MainPresenter internal constructor(
    private val prefs: PersistentPrefs,
    private val appUtil: AppUtil,
    private val accessState: AccessState,
    private val payloadManagerWiper: PayloadManagerWiper,
    private val payloadDataManager: PayloadDataManager,
    private val buyDataManager: BuyDataManager,
    private val dynamicFeeCache: DynamicFeeCache,
    private val exchangeRateFactory: ExchangeRateDataManager,
    private val rxBus: RxBus,
    private val feeDataManager: FeeDataManager,
    private val ethDataManager: EthDataManager,
    private val bchDataManager: BchDataManager,
    private val currencyState: CurrencyState,
    private val metadataManager: MetadataManager,
    private val stringUtils: StringUtils,
    private val shapeShiftDataManager: ShapeShiftDataManager,
    private val environmentSettings: EnvironmentConfig,
    private val coinifyDataManager: CoinifyDataManager,
    private val exchangeService: ExchangeService,
    private val kycStatusHelper: KycStatusHelper,
    private val lockboxDataManager: LockboxDataManager,
    private val deepLinkProcessor: DeepLinkProcessor,
    private val sunriverCampaignHelper: SunriverCampaignHelper,
    private val xlmDataManager: XlmDataManager,
    private val paxAccount: Erc20Account,
    private val pitFeatureFlag: FeatureFlag,
    private val pitABTestingExperiment: ABTestExperiment,
    private val pitLinking: PitLinking,
    private val nabuToken: NabuToken,
    private val nabuDataManager: NabuDataManager,
    private val crashLogger: CrashLogger
) : BasePresenter<MainView>() {

    internal val defaultCurrency: String
        get() = prefs.selectedFiatCurrency

    override fun onViewReady() {
        if (!accessState.isLoggedIn) {
            // This should never happen, but handle the scenario anyway by starting the launcher
            // activity, which handles all login/auth/corruption scenarios itself
            view.kickToLauncherPage()
        } else {
            logEvents()

            checkLockboxAvailability()

            view.showProgressDialog(R.string.please_wait)

            initMetadataElements()

            doPushNotifications()

            checkPitAvailability()

            setPitTitle()
        }
    }

    private fun setPitTitle() {
        compositeDisposable += pitABTestingExperiment.getABVariant(ABTestExperiment.AB_THE_PIT_SIDE_NAV_VARIANT).map {
            when (it) {
                "B" -> return@map stringUtils.getString(R.string.crypto_exchange)
                "C" -> return@map stringUtils.getString(R.string.crypto_trading)
                else -> return@map stringUtils.getString(R.string.the_pit_exchange_title)
            }
        }.subscribeBy { view.setPitItemTitle(it) }
    }

    private fun checkPitAvailability() {
        compositeDisposable += pitFeatureFlag.enabled.subscribeBy { view.setPitEnabled(it) }
    }

    private fun checkLockboxAvailability() {
        compositeDisposable += lockboxDataManager.isLockboxAvailable()
            .subscribe { enabled, _ -> view.displayLockboxMenu(enabled) }
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
            view.showTestnetWarning()
        }
    }

    private fun checkKycStatus() {
        compositeDisposable += kycStatusHelper.shouldDisplayKyc()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { view.enableSwapButton(it) },
                { Timber.e(it) }
            )
    }

    private fun setDebugExchangeVisibility() {
        if (BuildConfig.DEBUG) {
            view.showHomebrewDebugMenu()
        }
    }

    internal fun initMetadataElements() {
        compositeDisposable += metadataManager.attemptMetadataSetup()
            .andThen(exchangeRateCompletable())
            .andThen(ethCompletable())
            .andThen(shapeShiftCompletable())
            .andThen(bchCompletable())
            .andThen(feesCompletable())
            .observeOn(AndroidSchedulers.mainThread())
            .doAfterTerminate {
                view.hideProgressDialog()

                val strUri = prefs.getValue(PersistentPrefs.KEY_SCHEME_URL, "")
                if (strUri.isNotEmpty()) {
                    prefs.removeValue(PersistentPrefs.KEY_SCHEME_URL)
                    view.onHandleInput(strUri)
                }
            }
            .subscribe({
                checkKycStatus()
                setDebugExchangeVisibility()
                initBuyService()
                rxBus.emitEvent(MetadataEvent::class.java, MetadataEvent.SETUP_COMPLETE)

                checkForPendingLinks()
            }, { throwable ->
                if (throwable is InvalidCredentialsException || throwable is HDWalletException) {
                    if (payloadDataManager.isDoubleEncrypted) {
                        // Wallet double encrypted and needs to be decrypted to set up ether wallet, contacts etc
                        view.showSecondPasswordDialog()
                    } else {
                        logException(throwable)
                    }
                } else {
                    logException(throwable)
                }
            })
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
        compositeDisposable += deepLinkProcessor.getLink(view.getStartIntent())
            .filter { !view.shouldIgnoreDeepLinking() }
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
            is CampaignLinkState.WrongUri -> view.displayDialog(R.string.sunriver_invalid_url_title,
                R.string.sunriver_invalid_url_message)
            is CampaignLinkState.Data -> registerForCampaign(linkState.link.campaignData)
        }
    }

    private fun handleKycDeepLink(linkState: LinkState.KycDeepLink) {
        when (linkState.link) {
            is KycLinkState.Resubmit -> view.launchKyc(CampaignType.Resubmission)
            is KycLinkState.EmailVerified -> view.launchKyc(CampaignType.Swap)
            is KycLinkState.General -> {
                val data = linkState.link.campaignData
                if (data != null) {
                    registerForCampaign(data)
                } else {
                    view.launchKyc(CampaignType.Swap)
                }
            }
        }
    }

    private fun handleThePitDeepLink(linkState: LinkState.ThePitDeepLink) {
        view.launchThePitLinking(linkState.linkId)
    }

    private fun handleEmailVerifiedDeepLink(linkState: LinkState.EmailVerifiedDeepLink) {
        if (linkState.link === EmailVerifiedLinkState.FromPitLinking) {
            showThePitOrPitLinkingView(prefs.pitToWalletLinkId)
        }
    }

    private fun registerForCampaign(data: CampaignData) {
        compositeDisposable +=
            xlmDataManager.defaultAccount()
                .flatMapCompletable { account ->
                    sunriverCampaignHelper
                        .registerCampaignAndSignUpIfNeeded(account, data)
                }
                .andThen(kycStatusHelper.getKycStatus())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view.showProgressDialog(R.string.please_wait) }
                .doOnEvent { _, _ -> view.hideProgressDialog() }
                .subscribe({ status ->
                    prefs.setValue(SunriverCardType.JoinWaitList.javaClass.simpleName, true)
                    if (status != KycState.Verified) {
                        view.launchKyc(CampaignType.Sunriver)
                    } else {
                        view.refreshDashboard()
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
                        view.displayDialog(
                            R.string.sunriver_invalid_url_title,
                            errorMessageStringId
                        )
                    }
                }
                )
    }

    private fun bchCompletable(): Completable {
        return bchDataManager.initBchWallet(stringUtils.getString(R.string.bch_default_account_label))
            .doOnError { throwable ->
                crashLogger.logException(throwable)
                // TODO: 21/02/2018 Reload or disable?
                Timber.e(throwable, "Failed to load bch wallet")
            }
    }

    private fun ethCompletable(): Completable {
        return ethDataManager.initEthereumWallet(
            stringUtils.getString(R.string.eth_default_account_label),
            stringUtils.getString(R.string.pax_default_account_label)
        ).doOnError { throwable ->
            crashLogger.logException(throwable)
            // TODO: 21/02/2018 Reload or disable?
            Timber.e(throwable, "Failed to load eth wallet")
        }
    }

    private fun shapeShiftCompletable(): Completable {
        return shapeShiftDataManager.initShapeshiftTradeData()
            .onErrorComplete()
            .doOnError { throwable ->
                crashLogger.logException(throwable)
                // TODO: 21/02/2018 Reload or disable?
                Timber.e(throwable, "Failed to load shape shift trades")
            }
    }

    private fun logException(throwable: Throwable) {
        crashLogger.logException(throwable)
        view.showMetadataNodeFailure()
    }

    /**
     * All of these calls are allowed to fail here, we're just caching them in advance because we
     * can.
     */
    private fun feesCompletable(): Completable =
        feeDataManager.btcFeeOptions
            .doOnNext { dynamicFeeCache.btcFeeOptions = it }
            .ignoreElements()
            .onErrorComplete()
            .andThen(feeDataManager.ethFeeOptions
                .doOnNext { dynamicFeeCache.ethFeeOptions = it }
                .ignoreElements()
                .onErrorComplete()
            )
            .andThen(feeDataManager.bchFeeOptions
                .doOnNext { dynamicFeeCache.bchFeeOptions = it }
                .ignoreElements()
                .onErrorComplete()
            )
            .subscribeOn(Schedulers.io())

    private fun exchangeRateCompletable(): Completable {
        return exchangeRateFactory.updateTickers().applySchedulers()
    }

    internal fun unPair() {
        view.clearAllDynamicShortcuts()
        payloadManagerWiper.wipe()
        accessState.logout()
        accessState.unpairWallet()
        appUtil.restartApp(LauncherActivity::class.java)
        accessState.clearPin()
        buyDataManager.wipe()
        ethDataManager.clearEthAccountDetails()
        paxAccount.clear()
        bchDataManager.clearBchAccountDetails()
        DashboardPresenter.onLogout()
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
            Observables.zip(buyDataManager.canBuy,
                buyDataManager.isCoinifyAllowed).subscribe(
                { (isEnabled, isCoinifyAllowed) ->
                    view.setBuySellEnabled(isEnabled, isCoinifyAllowed)
                    if (isEnabled && !isCoinifyAllowed) {
                        compositeDisposable += buyDataManager.watchPendingTrades()
                            .applySchedulers()
                            .subscribe({ view.showTradeCompleteMsg(it) }, { it.printStackTrace() })

                        compositeDisposable += buyDataManager.webViewLoginDetails
                            .subscribe({ view.setWebViewLoginDetails(it) }, { it.printStackTrace() })
                    } else if (isEnabled && isCoinifyAllowed) {
                        notifyCompletedCoinifyTrades()
                    }
                }, { throwable ->
                    Timber.e(throwable)
                    view.setBuySellEnabled(enabled = false, useWebView = false)
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
                }) { view.showTradeCompleteMsg(it) }
    }

    internal fun decryptAndSetupMetadata(secondPassword: String) {
        if (!payloadDataManager.validateSecondPassword(secondPassword)) {
            view.showToast(R.string.invalid_password, ToastCustom.TYPE_ERROR)
            view.showSecondPasswordDialog()
        } else {
            compositeDisposable += metadataManager.decryptAndSetupMetadata(environmentSettings.bitcoinNetworkParameters,
                secondPassword)
                .subscribeBy(onError = { it.printStackTrace() },
                    onComplete = { appUtil.restartApp(LauncherActivity::class.java) })
        }
    }

    internal fun setCryptoCurrency(cryptoCurrency: CryptoCurrency) {
        currencyState.cryptoCurrency = cryptoCurrency
    }

    internal fun routeToBuySell() {
        compositeDisposable += buyDataManager.isCoinifyAllowed
            .subscribeBy(onError = { it.printStackTrace() },
                onNext = { coinifyAllowed ->
                    if (coinifyAllowed)
                        view.launchBuySell()
                })
    }

    internal fun clearLoginState() {
        accessState.logout()
    }

    internal fun startSwapOrKyc(targetCurrency: CryptoCurrency? /* = null*/) {
        val nabuUser = nabuToken.fetchNabuToken().flatMap {
            nabuDataManager.getUser(it)
        }
        compositeDisposable += nabuUser
            .subscribeBy(onError = { it.printStackTrace() }, onSuccess = { nabuUser ->
                if (nabuUser.tiers?.current ?: 0 > 0) {
                    view.launchSwap(
                        prefs.selectedFiatCurrency,
                        targetCurrency
                    )
                } else {
                    if (nabuUser.kycState == KycState.Rejected ||
                        nabuUser.kycState == KycState.UnderReview ||
                        prefs.swapIntroCompleted
                    )
                        view.launchKyc(CampaignType.Swap)
                    else
                        view.launchSwapIntro()
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
                    view.launchThePit()
                } else {
                    view.launchThePitLinking(linkId)
                }
            })
    }
}
