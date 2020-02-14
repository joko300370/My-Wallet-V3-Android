package com.blockchain.koin.modules

import android.content.Context
import com.blockchain.activities.StartSwap
import com.blockchain.network.websocket.Options
import com.blockchain.network.websocket.autoRetry
import com.blockchain.network.websocket.debugLog
import com.blockchain.network.websocket.newBlockchainWebSocket
import piuk.blockchain.android.ui.kyc.settings.KycStatusHelper
import com.blockchain.remoteconfig.CoinSelectionRemoteConfig
import com.blockchain.swap.nabu.datamanagers.custodialwalletimpl.PaymentAccountMapper
import com.blockchain.ui.CurrentContextAccess
import com.blockchain.ui.chooser.AccountListing
import com.blockchain.ui.password.SecondPasswordHandler
import com.google.gson.GsonBuilder
import info.blockchain.wallet.util.PrivateKeyFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import okhttp3.OkHttpClient
import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.data.api.bitpay.BitPayDataManager
import piuk.blockchain.android.data.api.bitpay.BitPayService
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager
import piuk.blockchain.android.data.coinswebsocket.strategy.CoinsWebSocketStrategy
import piuk.blockchain.android.deeplink.DeepLinkProcessor
import piuk.blockchain.android.deeplink.EmailVerificationDeepLinkHelper
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
import piuk.blockchain.android.ui.account.SecondPasswordHandlerDialog
import piuk.blockchain.android.ui.airdrops.AirdropCentrePresenter
import piuk.blockchain.android.ui.auth.FirebaseMobileNoticeRemoteConfig
import piuk.blockchain.android.ui.auth.MobileNoticeRemoteConfig
import piuk.blockchain.android.ui.auth.PinEntryPresenter
import piuk.blockchain.android.ui.backup.completed.BackupWalletCompletedPresenter
import piuk.blockchain.android.ui.backup.start.BackupWalletStartingPresenter
import piuk.blockchain.android.ui.backup.transfer.ConfirmFundsTransferPresenter
import piuk.blockchain.android.ui.backup.verify.BackupVerifyPresenter
import piuk.blockchain.android.ui.backup.wordlist.BackupWalletWordListPresenter
import piuk.blockchain.android.ui.buysell.coinify.signup.CoinifySignUpPresenter
import piuk.blockchain.android.ui.buysell.coinify.signup.identityinreview.CoinifyIdentityInReviewPresenter
import piuk.blockchain.android.ui.buysell.coinify.signup.invalidcountry.CoinifyInvalidCountryPresenter
import piuk.blockchain.android.ui.buysell.coinify.signup.selectcountry.CoinifySelectCountryPresenter
import piuk.blockchain.android.ui.buysell.coinify.signup.verifyemail.CoinifyVerifyEmailPresenter
import piuk.blockchain.android.ui.buysell.confirmation.buy.CoinifyBuyConfirmationPresenter
import piuk.blockchain.android.ui.buysell.confirmation.sell.CoinifySellConfirmationPresenter
import piuk.blockchain.android.ui.buysell.createorder.BuySellBuildOrderPresenter
import piuk.blockchain.android.ui.buysell.details.awaitingtransfer.CoinifyAwaitingBankTransferPresenter
import piuk.blockchain.android.ui.buysell.details.trade.CoinifyTransactionDetailPresenter
import piuk.blockchain.android.ui.buysell.overview.CoinifyOverviewPresenter
import piuk.blockchain.android.ui.buysell.payment.bank.accountoverview.BankAccountSelectionPresenter
import piuk.blockchain.android.ui.buysell.payment.bank.addaccount.AddBankAccountPresenter
import piuk.blockchain.android.ui.buysell.payment.bank.addaddress.AddAddressPresenter
import piuk.blockchain.android.ui.chooser.WalletAccountHelperAccountListingAdapter
import piuk.blockchain.android.ui.confirm.ConfirmPaymentPresenter
import piuk.blockchain.android.ui.createwallet.CreateWalletPresenter
import piuk.blockchain.android.ui.dashboard.DashboardInteractor
import piuk.blockchain.android.ui.dashboard.DashboardModel
import piuk.blockchain.android.ui.dashboard.DashboardState
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsCalculator
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper
import piuk.blockchain.android.ui.fingerprint.FingerprintPresenter
import piuk.blockchain.android.ui.home.MainPresenter
import piuk.blockchain.android.ui.home.MetadataLoader
import piuk.blockchain.android.ui.launcher.DeepLinkPersistence
import piuk.blockchain.android.ui.launcher.LauncherPresenter
import piuk.blockchain.android.ui.onboarding.OnboardingPresenter
import piuk.blockchain.android.ui.pairingcode.PairingCodePresenter
import piuk.blockchain.android.ui.receive.ReceivePresenter
import piuk.blockchain.android.ui.receive.ReceiveQrPresenter
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.ui.recover.RecoverFundsPresenter
import piuk.blockchain.android.ui.send.SendView
import piuk.blockchain.android.ui.send.SendPresenter
import piuk.blockchain.android.ui.send.strategy.BitcoinCashSendStrategy
import piuk.blockchain.android.ui.send.strategy.BitcoinSendStrategy
import piuk.blockchain.android.ui.send.strategy.EtherSendStrategy
import piuk.blockchain.android.ui.send.strategy.SendStrategy
import piuk.blockchain.android.ui.send.strategy.XlmSendStrategy
import piuk.blockchain.android.ui.send.strategy.PaxSendStrategy
import piuk.blockchain.android.ui.send.strategy.ResourceSendFundsResultLocalizer
import piuk.blockchain.android.ui.send.strategy.SendFundsResultLocalizer
import piuk.blockchain.android.ui.settings.SettingsPresenter
import piuk.blockchain.android.ui.ssl.SSLVerifyPresenter
import piuk.blockchain.android.ui.swap.SwapStarter
import piuk.blockchain.android.ui.swapintro.SwapIntroPresenter
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceivePresenter
import piuk.blockchain.android.ui.thepit.PitPermissionsPresenter
import piuk.blockchain.android.ui.thepit.PitVerifyEmailPresenter
import piuk.blockchain.android.ui.transactions.TransactionDetailPresenter
import piuk.blockchain.android.ui.transactions.TransactionHelper
import piuk.blockchain.android.ui.upgrade.UpgradeWalletPresenter
import piuk.blockchain.android.ui.transactions.TransactionsPresenter
import piuk.blockchain.android.util.BackupWalletUtil
import piuk.blockchain.android.util.OSUtil
import piuk.blockchain.android.util.PrngHelper
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.lifecycle.LifecycleInterestedComponent
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidcore.data.api.ConnectionApi
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.erc20.PaxAccount
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.utils.PrngFixer
import piuk.blockchain.androidcore.utils.SSLVerifyUtil
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcoreui.utils.DateUtil
import piuk.blockchain.androidcoreui.utils.OverlayDetection
import java.util.Locale

val applicationModule = applicationContext {

    factory { OSUtil(get()) }

    factory { StringUtils(get()) }

    bean {
        AppUtil(
            context = get(),
            payloadManager = get(),
            accessState = get(),
            prefs = get()
        )
    }

    factory { get<Context>().resources }

    factory { Locale.getDefault() }

    bean { CurrentContextAccess() }

    bean { LifecycleInterestedComponent() }

    context("Payload") {

        factory {
            EthDataManager(get(), get(), get(), get(), get(), get(), get(), get())
        }

        factory("pax") {
            PaxAccount(
                ethDataManager = get(),
                dataStore = get(),
                environmentSettings = get()
            ) as Erc20Account
        }

        factory {
            BchDataManager(get(), get(), get(), get(), get(), get(), get())
        }

        factory {
            BuyDataManager(
                settingsDataManager = get(),
                authDataManager = get(),
                payloadDataManager = get(),
                buyConditions = get(),
                exchangeService = get(),
                coinifyFeatureFlag = get("ff_coinify")
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
        }

        factory {
            SwipeToReceivePresenter(
                qrGenerator = get(),
                swipeToReceiveHelper = get()
            )
        }

        factory {
            AssetDetailsCalculator(
                locale = get()
            )
        }

        factory {
            WalletAccountHelper(
                payloadManager = get(),
                stringUtils = get(),
                currencyState = get(),
                ethDataManager = get(),
                bchDataManager = get(),
                xlmDataManager = get(),
                environmentSettings = get(),
                exchangeRates = get(),
                paxAccount = get("pax"),
                crashLogger = get()
            )
        }

        factory { WalletAccountHelperAccountListingAdapter(get()) }
            .bind(AccountListing::class)

        factory {
            SecondPasswordHandlerDialog(get(), get()) as SecondPasswordHandler
        }

        factory { KycStatusHelper(get(), get(), get(), get()) }

        factory { TransactionListDataManager(get(), get(), get(), get(), get(), get(), get()) }

        factory {
            FingerprintHelper(
                applicationContext = get(),
                prefs = get(),
                fingerprintAuth = get()
            )
        }

        bean {
            MetadataLoader(
                metadataManager = get(),
                payloadManagerWiper = get(),
                paxAccount = get(),
                buyDataManager = get(),
                shapeShiftDataManager = get(),
                dynamicFeeCache = get(),
                feeDataManager = get(),
                accessState = get(),
                appUtil = get(),
                rxBus = get(),
                crashLogger = get()
            )
        }

        factory {
            MainPresenter(
                prefs = get(),
                appUtil = get(),
                accessState = get(),
                metadataLoader = get(),
                payloadDataManager = get(),
                coinifyDataManager = get(),
                buyDataManager = get(),
                exchangeService = get(),
                exchangeRateFactory = get(),
                metadataManager = get(),
                currencyState = get(),
                environmentSettings = get(),
                kycStatusHelper = get(),
                lockboxDataManager = get(),
                deepLinkProcessor = get(),
                sunriverCampaignRegistration = get(),
                xlmDataManager = get(),
                pitFeatureFlag = get("ff_pit_linking"),
                pitLinking = get(),
                nabuDataManager = get(),
                nabuToken = get(),
                simpleBuySync = get(),
                crashLogger = get(),
                simpleBuyAvailability = get()
            )
        }

        factory("GBP") {
            GBPPaymentAccountMapper(stringUtils = get())
        }.bind(PaymentAccountMapper::class)

        factory("EUR") {
            EURPaymentAccountMapper(stringUtils = get())
        }.bind(PaymentAccountMapper::class)

        bean {
            CoinsWebSocketStrategy(
                coinsWebSocket = get(),
                ethDataManager = get(),
                swipeToReceiveHelper = get(),
                stringUtils = get(),
                gson = get(),
                erc20Account = get("pax"),
                payloadDataManager = get(),
                bchDataManager = get(),
                rxBus = get(),
                prefs = get(),
                currencyFormatManager = get(),
                appUtil = get(),
                accessState = get()
            )
        }

        factory {
            GsonBuilder().create()
        }

        factory {
            SimpleBuyAvailability(
                simpleBuyPrefs = get(),
                custodialWalletManager = get(),
                currencyPrefs = get(),
                simpleBuyFlag = get("ff_simple_buy")
            )
        }

        factory {
            OkHttpClient()
                .newBlockchainWebSocket(options = Options(url = BuildConfig.COINS_WEBSOCKET_URL))
                .autoRetry().debugLog("COIN_SOCKET")
        }

        factory {
            BuySellBuildOrderPresenter(
                coinifyDataManager = get(),
                sendDataManager = get(),
                exchangeService = get(),
                stringUtils = get(),
                currencyFormatManager = get(),
                exchangeRateDataManager = get(),
                feeDataManager = get(),
                dynamicFeeCache = get(),
                payloadDataManager = get(),
                nabuToken = get(),
                nabuDataManager = get(),
                coinSelectionRemoteConfig = get()
            )
        }

        factory {
            CoinifySellConfirmationPresenter(
                coinifyDataManager = get(),
                sendDataManager = get(),
                exchangeService = get(),
                stringUtils = get(),
                environmentConfig = get(),
                payloadDataManager = get(),
                lastTxUpdater = get(),
                coinSelectionRemoteConfig = get()
            )
        }

        factory {
            BankAccountSelectionPresenter(
                exchangeService = get(),
                coinifyDataManager = get()
            )
        }

        factory {
            CoinifyAwaitingBankTransferPresenter(
                exchangeService = get(),
                coinifyDataManager = get()
            )
        }

        factory {
            CoinifyAwaitingBankTransferPresenter(
                exchangeService = get(),
                coinifyDataManager = get()
            )
        }

        factory {
            AddBankAccountPresenter()
        }

        factory {
            CoinifyTransactionDetailPresenter(
                coinifyDataManager = get(),
                exchangeService = get()
            )
        }

        factory {
            CoinifyBuyConfirmationPresenter(
                payloadDataManager = get(),
                coinifyDataManager = get(),
                exchangeService = get(),
                stringUtils = get(),
                metadataManager = get(),
                currencyFormatUtil = get()
            )
        }

        factory {
            CoinifyIdentityInReviewPresenter(
                exchangeService = get(),
                coinifyDataManager = get()
            )
        }

        factory {
            CoinifyOverviewPresenter(
                exchangeService = get(),
                coinifyDataManager = get(),
                metadataManager = get(),
                stringUtils = get(),
                currencyFormatUtil = get()
            )
        }

        factory {
            CoinifyInvalidCountryPresenter()
        }

        factory {
            CoinifySelectCountryPresenter(
                buyDataManager = get()
            )
        }

        factory {
            CoinifyVerifyEmailPresenter(
                settingsDataManager = get(),
                walletOptionsDataManager = get(),
                coinifyDataManager = get(),
                payloadDataManager = get(),
                exchangeService = get(),
                metadataManager = get(),
                currencyState = get(),
                stringUtils = get()
            )
        }

        factory {
            SSLVerifyPresenter(
                sslVerifyUtil = get()
            )
        }

        factory {
            ChartsDataManager(
                historicPriceApi = get(),
                rxBus = get()
            )
        }

        factory {
            ConfirmFundsTransferPresenter(
                walletAccountHelper = get(),
                fundsDataManager = get(),
                payloadDataManager = get(),
                stringUtils = get(),
                currencyFormatManager = get()
            )
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
            AddAddressPresenter(
                coinifyDataManager = get(),
                exchangeService = get(),
                buyDataManager = get()
            )
        }

        factory {
            CoinifySignUpPresenter(
                coinifyDataManager = get(),
                exchangeService = get(),
                stringUtils = get()
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
                analytics = get()
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
            TransactionDetailPresenter(
                transactionHelper = get(),
                prefs = get(),
                payloadDataManager = get(),
                stringUtils = get(),
                transactionListDataManager = get(),
                exchangeRateDataManager = get(),
                bchDataManager = get(),
                ethDataManager = get(),
                environmentSettings = get(),
                xlmDataManager = get()
            )
        }

        factory<SendPresenter<SendView>> {
            SendPresenter(
                btcStrategy = get("BTCStrategy"),
                bchStrategy = get("BCHStrategy"),
                etherStrategy = get("EtherStrategy"),
                xlmStrategy = get("XLMStrategy"),
                paxStrategy = get("PaxStrategy"),
                prefs = get(),
                exchangeRates = get(),
                stringUtils = get(),
                envSettings = get(),
                exchangeRateFactory = get(),
                pitLinkingFeatureFlag = get("ff_pit_linking"),
                bitpayDataManager = get(),
                analytics = get()
            )
        }

        factory<SendStrategy<SendView>>("BTCStrategy") {
            BitcoinSendStrategy(
                walletAccountHelper = get(),
                payloadDataManager = get(),
                currencyState = get(),
                prefs = get(),
                exchangeRateFactory = get(),
                stringUtils = get(),
                sendDataManager = get(),
                dynamicFeeCache = get(),
                feeDataManager = get(),
                privateKeyFactory = get(),
                environmentSettings = get(),
                currencyFormatter = get(),
                exchangeRates = get(),
                coinSelectionRemoteConfig = get(),
                nabuDataManager = get(),
                nabuToken = get(),
                bitPayDataManager = get(),
                pitLinking = get(),
                analytics = get(),
                envSettings = get()
            )
        }

        factory<SendStrategy<SendView>>("BCHStrategy") {
            BitcoinCashSendStrategy(
                walletAccountHelper = get(),
                payloadDataManager = get(),
                prefs = get(),
                stringUtils = get(),
                sendDataManager = get(),
                dynamicFeeCache = get(),
                feeDataManager = get(),
                privateKeyFactory = get(),
                environmentSettings = get(),
                bchDataManager = get(),
                currencyFormatter = get(),
                exchangeRates = get(),
                environmentConfig = get(),
                currencyState = get(),
                coinSelectionRemoteConfig = get(),
                nabuToken = get(),
                nabuDataManager = get(),
                pitLinking = get(),
                envSettings = get(),
                analytics = get()
            )
        }

        factory<SendStrategy<SendView>>("EtherStrategy") {
            EtherSendStrategy(
                walletAccountHelper = get(),
                payloadDataManager = get(),
                ethDataManager = get(),
                stringUtils = get(),
                dynamicFeeCache = get(),
                feeDataManager = get(),
                currencyFormatter = get(),
                exchangeRates = get(),
                environmentConfig = get(),
                currencyState = get(),
                currencyPrefs = get(),
                nabuToken = get(),
                nabuDataManager = get(),
                pitLinking = get(),
                analytics = get()
            )
        }

        factory {
            ResourceSendFundsResultLocalizer(
                resources = get()
            )
        }.bind(SendFundsResultLocalizer::class)

        factory<SendStrategy<SendView>>("XLMStrategy") {
            XlmSendStrategy(
                currencyState = get(),
                xlmDataManager = get(),
                xlmFeesFetcher = get(),
                stringUtils = get(),
                walletOptionsDataManager = get(),
                xlmTransactionSender = get(),
                fiatExchangeRates = get(),
                sendFundsResultLocalizer = get(),
                nabuDataManager = get(),
                nabuToken = get(),
                pitLinking = get(),
                analytics = get()
            )
        }

        factory<SendStrategy<SendView>>("PaxStrategy") {
            PaxSendStrategy(
                walletAccountHelper = get(),
                payloadDataManager = get(),
                ethDataManager = get(),
                paxAccount = get("pax"),
                stringUtils = get(),
                dynamicFeeCache = get(),
                feeDataManager = get(),
                currencyFormatter = get(),
                exchangeRates = get(),
                environmentConfig = get(),
                currencyState = get(),
                currencyPrefs = get(),
                nabuToken = get(),
                nabuDataManager = get(),
                pitLinking = get(),
                analytics = get()
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
                fundsDataManager = get(),
                prefs = get(),
                appUtil = get(),
                privateKeyFactory = get(),
                environmentSettings = get(),
                currencyState = get(),
                analytics = get(),
                currencyFormatManager = get(),
                coinsWebSocketStrategy = get()
            )
        }

        factory {
            TransferFundsDataManager(
                /* payloadDataManager */ get(),
                /* sendDataManager */ get(),
                /* dynamicFeeCache */ get(),
                /* coinSelectionRemoteConfig */ get()
            )
        }

        factory {
            ReceiveQrPresenter(
                payloadDataManager = get(),
                qrCodeDataManager = get()
            )
        }

        factory { DeepLinkPersistence(get()) }

        factory { ConfirmPaymentPresenter() }

        factory {
            DashboardModel(
                initialState = DashboardState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                persistence = get()
            )
        }

        factory {
            DashboardInteractor(
                tokens = get(),
                payloadManager = get()
            )
        }

        factory {
            SimpleBuyInteractor(
                nabu = get(),
                tierService = get(),
                custodialWalletManager = get(),
                appUtil = get()
            )
        }

        factory {
            SimpleBuyModel(
                interactor = get(),
                scheduler = AndroidSchedulers.mainThread(),
                initialState = SimpleBuyState(),
                prefs = get(),
                gson = get()
            )
        }

        factory {
            SimpleBuyFlowNavigator(
                simpleBuyModel = get(),
                tierService = get()
            )
        }

        bean {
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
            TransactionsPresenter(
                exchangeRateDataManager = get(),
                transactionListDataManager = get(),
                ethDataManager = get(),
                paxAccount = get("pax"),
                payloadDataManager = get(),
                buyDataManager = get(),
                stringUtils = get(),
                prefs = get(),
                rxBus = get(),
                currencyState = get(),
                shapeShiftDataManager = get(),
                bchDataManager = get(),
                walletAccountHelper = get(),
                environmentSettings = get(),
                exchangeService = get(),
                coinifyDataManager = get(),
                fiatExchangeRates = get()
            )
        }

        factory {
            QrCodeDataManager()
        }

        factory {
            ReceivePresenter(
                prefs = get(),
                qrCodeDataManager = get(),
                walletAccountHelper = get(),
                payloadDataManager = get(),
                ethDataStore = get(),
                bchDataManager = get(),
                xlmDataManager = get(),
                environmentSettings = get(),
                currencyState = get(),
                fiatExchangeRates = get()
            )
        }

        factory { TransactionHelper(get(), get()) }

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
                /* swipeToReceiveHelper = */ get(),
                /* notificationTokenManager = */ get(),
                /* exchangeRateDataManager = */ get(),
                /* currencyFormatManager = */ get(),
                /* kycStatusHelper = */ get(),
                /* pitLinking = */ get(),
                /* analytics = */ get(),
                /*featureFlag = */get("ff_pit_linking")
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
                crashLogger = get()
            )
        }

        bean {
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
                retrofit = get("kotlin"),
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
                currencyFormatManager = get(),
                coinSelectionRemoteConfig = get()
            )
        }

        factory {
            BackupWalletCompletedPresenter(
                transferFundsDataManager = get(),
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
                featureFlag = get("ff_simple_buy"),
                custodialWalletManager = get(),
                currencyPrefs = get()
            )
        }

        factory {
            AirdropCentrePresenter(
                nabuToken = get(),
                nabu = get(),
                crashLogger = get()
            )
        }
    }

    factory {
        FirebaseMobileNoticeRemoteConfig(remoteConfig = get()) as MobileNoticeRemoteConfig
    }

    factory {
        OverlayDetection(prefs = get())
    }

    factory {
        SwapStarter(prefs = get())
    }.bind(StartSwap::class)

    factory { DateUtil(get()) }

    bean {
        PrngHelper(
            context = get(),
            accessState = get()
        )
    }.bind(PrngFixer::class)

    factory { PrivateKeyFactory() }

    bean { DynamicFeeCache() }

    factory { CoinSelectionRemoteConfig(get()) }

    factory { RecoverFundsPresenter() }

    bean {
        ConnectionApi(retrofit = get("explorer"))
    }

    bean {
        ConnectionApi(retrofit = get("explorer"))
    }

    bean {
        SSLVerifyUtil(rxBus = get(), connectionApi = get())
    }
}
