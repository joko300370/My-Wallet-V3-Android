package com.blockchain.koin.modules

import android.content.Context
import com.blockchain.accounts.AccountList
import com.blockchain.accounts.AsyncAllAccountList
import com.blockchain.koin.bch
import com.blockchain.koin.btc
import com.blockchain.koin.cardPaymentsFeatureFlag
import com.blockchain.koin.eth
import com.blockchain.koin.eur
import com.blockchain.koin.explorerRetrofit
import com.blockchain.koin.gbp
import com.blockchain.koin.interestAccountFeatureFlag
import com.blockchain.koin.moshiExplorerRetrofit
import com.blockchain.koin.newSwapFeatureFlag
import com.blockchain.koin.pax
import com.blockchain.koin.paxAccount
import com.blockchain.koin.payloadScope
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.pitFeatureFlag
import com.blockchain.koin.sellFeatureFlag
import com.blockchain.koin.simpleBuyFeatureFlag
import com.blockchain.koin.simpleBuyFundsFeatureFlag
import com.blockchain.koin.usdt
import com.blockchain.koin.usdtAccount
import com.blockchain.koin.xlm
import com.blockchain.logging.DigitalTrust
import com.blockchain.network.websocket.Options
import com.blockchain.network.websocket.autoRetry
import com.blockchain.network.websocket.debugLog
import com.blockchain.network.websocket.newBlockchainWebSocket
import com.blockchain.remoteconfig.CoinSelectionRemoteConfig
import com.blockchain.swap.nabu.datamanagers.custodialwalletimpl.PaymentAccountMapper
import com.blockchain.ui.password.SecondPasswordHandler
import com.blockchain.wallet.DefaultLabels
import com.google.gson.GsonBuilder
import info.blockchain.balance.CryptoCurrency
import io.reactivex.android.schedulers.AndroidSchedulers
import okhttp3.OkHttpClient
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.accounts.AsyncAllAccountListImplementation
import piuk.blockchain.android.accounts.BchAccountListAdapter
import piuk.blockchain.android.accounts.BtcAccountListAdapter
import piuk.blockchain.android.accounts.EthAccountListAdapter
import piuk.blockchain.android.accounts.PaxAccountListAdapter
import piuk.blockchain.android.accounts.UsdtAccountListAdapter
import piuk.blockchain.android.cards.CardModel
import piuk.blockchain.android.cards.partners.EverypayCardActivator
import piuk.blockchain.android.data.api.bitpay.BitPayDataManager
import piuk.blockchain.android.data.api.bitpay.BitPayService
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.data.coinswebsocket.service.CoinsWebSocketService
import piuk.blockchain.android.data.coinswebsocket.strategy.CoinsWebSocketStrategy
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager
import piuk.blockchain.android.deeplink.DeepLinkProcessor
import piuk.blockchain.android.deeplink.EmailVerificationDeepLinkHelper
import piuk.blockchain.android.identity.SiftDigitalTrust
import piuk.blockchain.android.kyc.KycDeepLinkHelper
import piuk.blockchain.android.simplebuy.EURPaymentAccountMapper
import piuk.blockchain.android.simplebuy.GBPPaymentAccountMapper
import piuk.blockchain.android.simplebuy.SimpleBuyAvailability
import piuk.blockchain.android.simplebuy.SimpleBuyFlowNavigator
import piuk.blockchain.android.simplebuy.SimpleBuyInflateAdapter
import piuk.blockchain.android.simplebuy.SimpleBuyInteractor
import piuk.blockchain.android.simplebuy.SimpleBuyModel
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.sunriver.SunriverDeepLinkHelper
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.android.thepit.PitLinkingImpl
import piuk.blockchain.android.thepit.ThePitDeepLinkParser
import piuk.blockchain.android.ui.account.AccountEditPresenter
import piuk.blockchain.android.ui.account.AccountPresenter
import piuk.blockchain.android.ui.account.ConfirmPaymentPresenter
import piuk.blockchain.android.ui.account.SecondPasswordHandlerDialog
import piuk.blockchain.android.ui.airdrops.AirdropCentrePresenter
import piuk.blockchain.android.ui.auth.FirebaseMobileNoticeRemoteConfig
import piuk.blockchain.android.ui.auth.MobileNoticeRemoteConfig
import piuk.blockchain.android.ui.auth.PinEntryPresenter
import piuk.blockchain.android.ui.backup.completed.BackupWalletCompletedPresenter
import piuk.blockchain.android.ui.backup.start.BackupWalletStartingPresenter
import piuk.blockchain.android.ui.backup.verify.BackupVerifyPresenter
import piuk.blockchain.android.ui.backup.wordlist.BackupWalletWordListPresenter
import piuk.blockchain.android.ui.chooser.WalletAccountHelper
import piuk.blockchain.android.ui.createwallet.CreateWalletPresenter
import piuk.blockchain.android.ui.customviews.SwapTrendingPairsProvider
import piuk.blockchain.android.ui.customviews.TrendingPairsProvider
import piuk.blockchain.android.ui.dashboard.BalanceAnalyticsReporter
import piuk.blockchain.android.ui.dashboard.DashboardInteractor
import piuk.blockchain.android.ui.dashboard.DashboardModel
import piuk.blockchain.android.ui.dashboard.DashboardState
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsInteractor
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsModel
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsState
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper
import piuk.blockchain.android.ui.fingerprint.FingerprintPresenter
import piuk.blockchain.android.ui.home.CacheCredentialsWiper
import piuk.blockchain.android.ui.home.CredentialsWiper
import piuk.blockchain.android.ui.home.MainPresenter
import piuk.blockchain.android.ui.kyc.settings.KycStatusHelper
import piuk.blockchain.android.ui.launcher.DeepLinkPersistence
import piuk.blockchain.android.ui.launcher.LauncherPresenter
import piuk.blockchain.android.ui.launcher.Prerequisites
import piuk.blockchain.android.ui.onboarding.OnboardingPresenter
import piuk.blockchain.android.ui.pairingcode.PairingCodePresenter
import piuk.blockchain.android.ui.recover.RecoverFundsPresenter
import piuk.blockchain.android.ui.sell.BuySellFlowNavigator
import piuk.blockchain.android.ui.settings.SettingsPresenter
import piuk.blockchain.android.ui.shortcuts.receive.ReceiveQrPresenter
import piuk.blockchain.android.ui.ssl.SSLVerifyPresenter
import piuk.blockchain.android.ui.swap.SwapTypeSwitcher
import piuk.blockchain.android.ui.swapintro.SwapIntroPresenter
import piuk.blockchain.android.ui.swipetoreceive.AddressGenerator
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceivePresenter
import piuk.blockchain.android.ui.thepit.PitPermissionsPresenter
import piuk.blockchain.android.ui.thepit.PitVerifyEmailPresenter
import piuk.blockchain.android.ui.transfer.receive.activity.ReceivePresenter
import piuk.blockchain.android.ui.upgrade.UpgradeWalletPresenter
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.BackupWalletUtil
import piuk.blockchain.android.util.CurrentContextAccess
import piuk.blockchain.android.util.OSUtil
import piuk.blockchain.android.util.PrngHelper
import piuk.blockchain.android.util.ResourceDefaultLabels
import piuk.blockchain.android.util.RootUtil
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.lifecycle.LifecycleInterestedComponent
import piuk.blockchain.android.withdraw.mvi.WithdrawInteractor
import piuk.blockchain.android.withdraw.mvi.WithdrawModel
import piuk.blockchain.android.withdraw.mvi.WithdrawStatePersistence
import piuk.blockchain.androidcore.data.api.ConnectionApi
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.erc20.PaxAccount
import piuk.blockchain.androidcore.data.erc20.UsdtAccount
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.utils.PrngFixer
import piuk.blockchain.androidcore.utils.SSLVerifyUtil
import piuk.blockchain.androidcoreui.utils.DateUtil

val applicationModule = module {

    factory { OSUtil(get()) }

    factory { StringUtils(get()) }

    single {
        AppUtil(
            context = get(),
            payloadManager = get(),
            accessState = get(),
            prefs = get()
        )
    }

    factory { RootUtil() }

    single {
        CoinsWebSocketService(
            applicationContext = get()
        )
    }

    factory { get<Context>().resources }

    single { CurrentContextAccess() }

    single { LifecycleInterestedComponent() }

    single {
        SiftDigitalTrust(
            accountId = BuildConfig.SIFT_ACCOUNT_ID,
            beaconKey = BuildConfig.SIFT_BEACON_KEY
        )
    }.bind(DigitalTrust::class)

    scope(payloadScopeQualifier) {
        factory {
            EthDataManager(
                payloadDataManager = get(),
                ethAccountApi = get(),
                ethDataStore = get(),
                walletOptionsDataManager = get(),
                metadataManager = get(),
                environmentSettings = get(),
                lastTxUpdater = get(),
                rxBus = get()
            )
        }

        factory(paxAccount) {
            PaxAccount(
                ethDataManager = get(),
                dataStore = get(),
                environmentSettings = get()
            )
        }.bind(Erc20Account::class)

        factory(usdtAccount) {
            UsdtAccount(
                ethDataManager = get(),
                dataStore = get(),
                environmentSettings = get()
            )
        }.bind(Erc20Account::class)

        factory {
            BchDataManager(
                payloadDataManager = get(),
                bchDataStore = get(),
                environmentSettings = get(),
                blockExplorer = get(),
                defaultLabels = get(),
                metadataManager = get(),
                rxBus = get()
            )
        }

        factory {
            SwipeToReceiveHelper(
                payloadDataManager = get(),
                prefs = get(),
                ethDataManager = get(),
                bchDataManager = get(),
                stringUtils = get(),
                environmentSettings = get(),
                xlmDataManager = get()
            )
        }.bind(AddressGenerator::class)

        factory {
            SwipeToReceivePresenter(
                qrGenerator = get(),
                swipeToReceiveHelper = get()
            )
        }

        factory {
            WalletAccountHelper(
                payloadManager = get()
            )
        }

        factory {
            SecondPasswordHandlerDialog(get(), get())
        }.bind(SecondPasswordHandler::class)

        factory { KycStatusHelper(get(), get(), get(), get()) }

        factory {
            FingerprintHelper(
                applicationContext = get(),
                prefs = get(),
                fingerprintAuth = get()
            )
        }

        scoped {
            CredentialsWiper(
                payloadManagerWiper = get(),
                paxAccount = get(paxAccount),
                usdtAccount = get(usdtAccount),
                accessState = get(),
                appUtil = get()
            )
        }

        factory {
            CacheCredentialsWiper(
                ethDataManager = get(),
                bchDataManager = get(),
                metadataManager = get(),
                walletOptionsState = get(),
                nabuDataManager = get()
            )
        }

        factory {
            MainPresenter(
                prefs = get(),
                accessState = get(),
                credentialsWiper = get(),
                payloadDataManager = get(),
                exchangeRateFactory = get(),
                environmentSettings = get(),
                kycStatusHelper = get(),
                lockboxDataManager = get(),
                deepLinkProcessor = get(),
                sunriverCampaignRegistration = get(),
                xlmDataManager = get(),
                pitFeatureFlag = get(pitFeatureFlag),
                pitLinking = get(),
                nabuDataManager = get(),
                nabuToken = get(),
                simpleBuySync = get(),
                crashLogger = get(),
                cacheCredentialsWiper = get(),
                analytics = get()
            )
        }

        factory(gbp) {
            GBPPaymentAccountMapper(stringUtils = get())
        }.bind(PaymentAccountMapper::class)

        factory(eur) {
            EURPaymentAccountMapper(stringUtils = get())
        }.bind(PaymentAccountMapper::class)

        scoped {
            CoinsWebSocketStrategy(
                coinsWebSocket = get(),
                ethDataManager = get(),
                swipeToReceiveHelper = get(),
                stringUtils = get(),
                gson = get(),
                paxAccount = get(paxAccount),
                usdtAccount = get(usdtAccount),
                payloadDataManager = get(),
                bchDataManager = get(),
                rxBus = get(),
                prefs = get(),
                appUtil = get(),
                accessState = get()
            )
        }

        factory {
            GsonBuilder().create()
        }

        factory {
            SimpleBuyAvailability(
                simpleBuyFlag = get(simpleBuyFeatureFlag)
            )
        }

        factory {
            OkHttpClient()
                .newBlockchainWebSocket(options = Options(url = BuildConfig.COINS_WEBSOCKET_URL))
                .autoRetry().debugLog("COIN_SOCKET")
        }

        factory {
            UpgradeWalletPresenter(
                prefs = get(),
                appUtil = get(),
                accessState = get(),
                stringUtils = get(),
                authDataManager = get(),
                payloadDataManager = get(),
                crashLogger = get()
            )
        }

        factory {
            PairingCodePresenter(
                qrCodeDataManager = get(),
                stringUtils = get(),
                payloadDataManager = get(),
                authDataManager = get()
            )
        }

        factory {
            CreateWalletPresenter(
                payloadDataManager = get(),
                prefs = get(),
                appUtil = get(),
                accessState = get(),
                prngFixer = get(),
                analytics = get(),
                walletPrefs = get()
            )
        }

        factory {
            BackupWalletStartingPresenter(
                payloadDataManager = get()
            )
        }

        factory {
            BackupWalletWordListPresenter(
                backupWalletUtil = get()
            )
        }

        factory {
            BackupWalletUtil(
                payloadDataManager = get(),
                environmentConfig = get()
            )
        }

        factory {
            FingerprintPresenter(
                fingerprintHelper = get()
            )
        }

        factory {
            BackupVerifyPresenter(
                payloadDataManager = get(),
                backupWalletUtil = get(),
                walletStatus = get()
            )
        }

        factory {
            SunriverDeepLinkHelper(
                linkHandler = get()
            )
        }

        factory {
            KycDeepLinkHelper(
                linkHandler = get()
            )
        }

        factory {
            ThePitDeepLinkParser()
        }

        factory {
            SwapIntroPresenter(prefs = get())
        }

        factory { EmailVerificationDeepLinkHelper() }

        factory {
            DeepLinkProcessor(
                linkHandler = get(),
                kycDeepLinkHelper = get(),
                sunriverDeepLinkHelper = get(),
                emailVerifiedLinkHelper = get(),
                thePitDeepLinkParser = get()
            )
        }

        factory {
            AccountPresenter(
                payloadDataManager = get(),
                bchDataManager = get(),
                metadataManager = get(),
                appUtil = get(),
                privateKeyFactory = get(),
                environmentSettings = get(),
                analytics = get(),
                coinsWebSocketStrategy = get(),
                coincore = get(),
                sendDataManager = get(),
                feeDataManager = get(),
                exchangeRates = get(),
                environmentConfig = get(),
                walletPreferences = get(),
                custodialWalletManager = get()
            )
        }

        factory {
            ConfirmPaymentPresenter()
        }

        factory {
            ReceiveQrPresenter(
                payloadDataManager = get(),
                qrCodeDataManager = get()
            )
        }

        factory { DeepLinkPersistence(get()) }

        factory {
            DashboardModel(
                initialState = DashboardState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get()
            )
        }

        factory {
            DashboardInteractor(
                coincore = get(),
                payloadManager = get(),
                exchangeRates = get(),
                currencyPrefs = get(),
                custodialWalletManager = get(),
                simpleBuyPrefs = get(),
                analytics = get(),
                crashLogger = get()
            )
        }

        scoped {
            AssetDetailsModel(
                initialState = AssetDetailsState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get()
            )
        }

        factory {
            AssetDetailsInteractor(
                interestFeatureFlag = get(interestAccountFeatureFlag),
                dashboardPrefs = get(),
                coincore = get()
            )
        }

        factory {
            SwapTypeSwitcher(
                newSwapFeatureFlag = get(newSwapFeatureFlag)
            )
        }

        factory {
            SimpleBuyInteractor(
                withdrawLocksRepository = get(),
                tierService = get(),
                custodialWalletManager = get(),
                appUtil = get(),
                coincore = get(),
                eligibilityProvider = get()
            )
        }

        factory {
            SimpleBuyModel(
                interactor = get(),
                scheduler = AndroidSchedulers.mainThread(),
                initialState = SimpleBuyState(),
                ratingPrefs = get(),
                prefs = get(),
                gson = get(),
                cardActivators = listOf(
                    EverypayCardActivator(get(), get())
                )
            )
        }

        scoped {
            WithdrawStatePersistence()
        }

        factory {
            WithdrawInteractor(
                assetBalancesRepository = get(),
                custodialWalletManager = get()
            )
        }

        factory {
            WithdrawModel(
                withdrawStatePersistence = get(),
                mainScheduler = AndroidSchedulers.mainThread(),
                withdrawInteractor = get()
            )
        }

        factory {
            CardModel(
                interactor = get(),
                currencyPrefs = get(),
                scheduler = AndroidSchedulers.mainThread(),
                cardActivators = listOf(
                    EverypayCardActivator(get(), get())
                ),
                gson = get(),
                prefs = get()
            )
        }

        factory {
            SimpleBuyFlowNavigator(
                simpleBuyModel = get(),
                tierService = get(),
                currencyPrefs = get(),
                custodialWalletManager = get()
            )
        }

        factory {
            BuySellFlowNavigator(
                simpleBuyModel = get(),
                custodialWalletManager = get(),
                currencyPrefs = get(),
                sellFeatureFlag = get(sellFeatureFlag),
                tierService = get(),
                eligibilityProvider = get()
            )
        }

        scoped {
            val inflateAdapter = SimpleBuyInflateAdapter(
                prefs = get(),
                gson = get()
            )

            SimpleBuySyncFactory(
                custodialWallet = get(),
                availabilityChecker = get(),
                localStateAdapter = inflateAdapter
            )
        }

        factory {
            BalanceAnalyticsReporter(
                analytics = get()
            )
        }

        factory {
            QrCodeDataManager()
        }

        factory {
            ReceivePresenter(
                prefs = get(),
                qrCodeDataManager = get(),
                exchangeRates = get()
            )
        }

        factory {
            SettingsPresenter(
                /* fingerprintHelper = */ get(),
                /* authDataManager = */ get(),
                /* settingsDataManager = */ get(),
                /* emailUpdater = */ get(),
                /* payloadManager = */ get(),
                /* payloadDataManager = */ get(),
                /* stringUtils = */ get(),
                /* prefs = */ get(),
                /* accessState = */ get(),
                /* custodialWalletManager = */ get(),
                /* swipeToReceiveHelper = */ get(),
                /* notificationTokenManager = */ get(),
                /* exchangeRateDataManager = */ get(),
                /* kycStatusHelper = */ get(),
                /* pitLinking = */ get(),
                /* analytics = */ get(),
                /*featureFlag = */get(pitFeatureFlag),
                /*featureFlag = */get(cardPaymentsFeatureFlag),
                /*featureFlag = */get(simpleBuyFundsFeatureFlag),
                /*simpleBuyPrefs = */get()
            )
        }

        factory {
            PinEntryPresenter(
                authDataManager = get(),
                appUtil = get(),
                prefs = get(),
                payloadDataManager = get(),
                stringUtils = get(),
                fingerprintHelper = get(),
                accessState = get(),
                walletOptionsDataManager = get(),
                environmentSettings = get(),
                prngFixer = get(),
                mobileNoticeRemoteConfig = get(),
                crashLogger = get(),
                analytics = get()
            )
        }

        scoped {
            PitLinkingImpl(
                nabu = get(),
                nabuToken = get(),
                payloadDataManager = get(),
                ethDataManager = get(),
                bchDataManager = get(),
                xlmDataManager = get()
            )
        }.bind(PitLinking::class)

        factory {
            BitPayDataManager(
                bitPayService = get()
            )
        }

        factory {
            BitPayService(
                environmentConfig = get(),
                retrofit = get(moshiExplorerRetrofit),
                rxBus = get()
            )
        }

        factory {
            PitPermissionsPresenter(
                nabu = get(),
                nabuToken = get(),
                pitLinking = get(),
                analytics = get(),
                prefs = get()
            )
        }

        factory {
            PitVerifyEmailPresenter(
                nabuToken = get(),
                nabu = get(),
                emailSyncUpdater = get()
            )
        }

        factory {
            AccountEditPresenter(
                prefs = get(),
                stringUtils = get(),
                payloadDataManager = get(),
                bchDataManager = get(),
                metadataManager = get(),
                sendDataManager = get(),
                privateKeyFactory = get(),
                swipeToReceiveHelper = get(),
                dynamicFeeCache = get(),
                environmentSettings = get(),
                analytics = get(),
                exchangeRates = get(),
                coinSelectionRemoteConfig = get()
            )
        }

        factory {
            BackupWalletCompletedPresenter(
                walletStatus = get()
            )
        }

        factory {
            OnboardingPresenter(
                fingerprintHelper = get(),
                accessState = get(),
                settingsDataManager = get()
            )
        }

        factory {
            LauncherPresenter(
                appUtil = get(),
                payloadDataManager = get(),
                prefs = get(),
                deepLinkPersistence = get(),
                accessState = get(),
                settingsDataManager = get(),
                notificationTokenManager = get(),
                envSettings = get(),
                featureFlag = get(simpleBuyFeatureFlag),
                currencyPrefs = get(),
                analytics = get(),
                crashLogger = get(),
                prerequisites = get()
            )
        }

        factory {
            Prerequisites(
                metadataManager = get(),
                settingsDataManager = get(),
                coincore = get(),
                crashLogger = get(),
                dynamicFeeCache = get(),
                feeDataManager = get(),
                simpleBuySync = get(),
                walletApi = get(),
                addressGenerator = get(),
                payloadDataManager = get(),
                rxBus = get()
            )
        }

        factory {
            AirdropCentrePresenter(
                nabuToken = get(),
                nabu = get(),
                crashLogger = get()
            )
        }

        factory(btc) { BtcAccountListAdapter(get()) }.bind(AccountList::class)
        factory(bch) { BchAccountListAdapter(get()) }.bind(AccountList::class)
        factory(eth) { EthAccountListAdapter(get()) }.bind(AccountList::class)
        factory(pax) { PaxAccountListAdapter(get(), get()) }.bind(AccountList::class)
        factory(usdt) { UsdtAccountListAdapter(get(), get()) }.bind(AccountList::class)

        factory {
            AsyncAllAccountListImplementation(
                mapOf(
                    CryptoCurrency.BTC to get(btc),
                    CryptoCurrency.ETHER to get(eth),
                    CryptoCurrency.BCH to get(bch),
                    CryptoCurrency.XLM to get(xlm),
                    CryptoCurrency.PAX to get(pax),
                    CryptoCurrency.USDT to get(usdt)
                )
            )
        }.bind(AsyncAllAccountList::class)
    }

    factory {
        FirebaseMobileNoticeRemoteConfig(remoteConfig = get())
    }.bind(MobileNoticeRemoteConfig::class)

    factory {
        SwapTrendingPairsProvider(
            coincore = payloadScope.get(),
            eligibilityProvider = payloadScope.get()
        )
    }.bind(TrendingPairsProvider::class)

    factory { DateUtil(get()) }

    single {
        PrngHelper(
            context = get(),
            accessState = get()
        )
    }.bind(PrngFixer::class)

    single { DynamicFeeCache() }

    factory { CoinSelectionRemoteConfig(get()) }

    factory { RecoverFundsPresenter() }

    single {
        ConnectionApi(retrofit = get(explorerRetrofit))
    }

    single {
        SSLVerifyUtil(rxBus = get(), connectionApi = get())
    }

    factory {
        SSLVerifyPresenter(
            sslVerifyUtil = get()
        )
    }

    factory { ResourceDefaultLabels(get()) }.bind(DefaultLabels::class)
}
