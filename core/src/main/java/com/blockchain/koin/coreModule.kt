@file:Suppress("USELESS_CAST")

package com.blockchain.koin

import android.app.backup.BackupManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.preference.PreferenceManager
import com.blockchain.datamanagers.AccountLookup
import com.blockchain.datamanagers.AddressResolver
import com.blockchain.datamanagers.DataManagerPayloadDecrypt
import com.blockchain.datamanagers.MaximumSpendableCalculator
import com.blockchain.datamanagers.SelfFeeCalculatingTransactionExecutor
import com.blockchain.datamanagers.TransactionExecutor
import com.blockchain.datamanagers.TransactionExecutorViaDataManagers
import com.blockchain.datamanagers.TransactionExecutorWithoutFees
import com.blockchain.fees.FeeType
import com.blockchain.logging.LastTxUpdateDateOnSettingsService
import com.blockchain.logging.LastTxUpdater
import com.blockchain.logging.NullLogger
import com.blockchain.logging.TimberLogger
import com.blockchain.metadata.MetadataRepository
import com.blockchain.payload.PayloadDecrypt
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.NotificationPrefs
import com.blockchain.preferences.DashboardPrefs
import com.blockchain.preferences.SecurityPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.preferences.ThePitLinkingPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.sunriver.XlmHorizonUrlFetcher
import com.blockchain.sunriver.XlmTransactionTimeoutFetcher
import com.blockchain.wallet.SeedAccess
import com.blockchain.wallet.SeedAccessWithoutPrompt
import info.blockchain.api.blockexplorer.BlockExplorer
import info.blockchain.wallet.metadata.MetadataDerivation
import info.blockchain.wallet.util.PrivateKeyFactory
import org.bitcoinj.params.BitcoinMainNetParams
import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.util.RootUtil
import piuk.blockchain.androidcore.BuildConfig
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.access.AccessStateImpl
import piuk.blockchain.androidcore.data.access.LogoutTimer
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.auth.AuthService
import piuk.blockchain.androidcore.data.bitcoincash.BchDataStore
import piuk.blockchain.androidcore.data.erc20.datastores.Erc20DataStore
import piuk.blockchain.androidcore.data.ethereum.EthereumAccountWrapper
import piuk.blockchain.androidcore.data.ethereum.datastores.EthDataStore
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateService
import piuk.blockchain.androidcore.data.exchangerate.datastore.ExchangeRateDataStore
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.metadata.MoshiMetadataRepositoryAdapter
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManagerSeedAccessAdapter
import piuk.blockchain.androidcore.data.payload.PayloadService
import piuk.blockchain.androidcore.data.payload.PromptingSeedAccessAdapter
import piuk.blockchain.androidcore.data.payments.PaymentService
import piuk.blockchain.androidcore.data.payments.SendDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater
import piuk.blockchain.androidcore.data.settings.PhoneNumberUpdater
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.data.settings.SettingsEmailAndSyncUpdater
import piuk.blockchain.androidcore.data.settings.SettingsPhoneNumberUpdater
import piuk.blockchain.androidcore.data.settings.SettingsPhoneVerificationQuery
import piuk.blockchain.androidcore.data.settings.SettingsService
import piuk.blockchain.androidcore.data.settings.applyFlag
import piuk.blockchain.androidcore.data.settings.datastore.SettingsDataStore
import piuk.blockchain.androidcore.data.settings.datastore.SettingsMemoryStore
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsState
import piuk.blockchain.androidcore.utils.AESUtilWrapper
import piuk.blockchain.androidcore.utils.DeviceIdGenerator
import piuk.blockchain.androidcore.utils.DeviceIdGeneratorImpl
import piuk.blockchain.androidcore.utils.PrefsUtil
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.UUIDGenerator
import java.util.UUID

val coreModule = applicationContext {

    bean { RxBus() }

    factory { AuthService(get(), get()) }

    factory { PrivateKeyFactory() }

    factory { RootUtil() }

    context("Payload") {

        factory { PayloadService(get()) }

        factory { PayloadDataManager(get(), get(), get(), get(), get()) }

        factory { DataManagerPayloadDecrypt(get(), get()) as PayloadDecrypt }

        factory { PromptingSeedAccessAdapter(PayloadDataManagerSeedAccessAdapter(get()), get()) }
            .bind(SeedAccessWithoutPrompt::class)
            .bind(SeedAccess::class)

        bean { MetadataManager(get(), get(), MetadataDerivation(BitcoinMainNetParams.get())) }

        bean { MoshiMetadataRepositoryAdapter(get(), get()) as MetadataRepository }

        factory { AddressResolver(get(), get(), get()) }

        factory { AccountLookup(get(), get(), get()) }

        factory {
            TransactionExecutorViaDataManagers(
                payloadDataManager = get(),
                ethDataManager = get(),
                erc20Account = get(),
                sendDataManager = get(),
                addressResolver = get(),
                accountLookup = get(),
                defaultAccountDataManager = get(),
                ethereumAccountWrapper = get(),
                xlmSender = get(),
                coinSelectionRemoteConfig = get(),
                analytics = get()
            ) as TransactionExecutor
        }

        factory("Regular") {
            SelfFeeCalculatingTransactionExecutor(
                get(),
                get(),
                get(),
                FeeType.Regular
            ) as TransactionExecutorWithoutFees
        }

        factory("Priority") {
            SelfFeeCalculatingTransactionExecutor(
                get(),
                get(),
                get(),
                FeeType.Priority
            ) as TransactionExecutorWithoutFees
        }

        factory("Regular") {
            get<TransactionExecutorWithoutFees>("Regular") as MaximumSpendableCalculator
        }

        factory("Priority") {
            get<TransactionExecutorWithoutFees>("Priority") as MaximumSpendableCalculator
        }

        bean { EthDataStore() }

        bean { Erc20DataStore() }

        bean { BchDataStore() }

        bean { WalletOptionsState() }

        bean { SettingsDataManager(get(), get(), get()) }

        bean { SettingsService(get()) }

        bean {
            SettingsDataStore(SettingsMemoryStore(), get<SettingsService>().getSettingsObservable())
        }

        factory { WalletOptionsDataManager(get(), get(), get(), get("explorer-url")) }
            .bind(XlmTransactionTimeoutFetcher::class).bind(XlmHorizonUrlFetcher::class)

        factory { ExchangeRateDataManager(get(), get()) }

        bean { ExchangeRateDataStore(get(), get()) }

        factory { FeeDataManager(get(), get(), get()) }

        factory {
            AuthDataManager(
                prefs = get(),
                authService = get(),
                accessState = get(),
                aesUtilWrapper = get(),
                prngHelper = get(),
                crashLogger = get()
            )
        }

        factory { LastTxUpdateDateOnSettingsService(get()) as LastTxUpdater }

        factory { SendDataManager(get(), get(), get()) }

        factory { SettingsPhoneVerificationQuery(get()).applyFlag(get("ff_sms_verification")) }

        factory { SettingsPhoneNumberUpdater(get()) as PhoneNumberUpdater }

        factory { SettingsEmailAndSyncUpdater(get(), get()) as EmailSyncUpdater }
    }

    bean { BlockExplorer(get("explorer"), get("api"), getProperty("api-code")) }

    factory { ExchangeRateService(get()) }

    factory {
        DeviceIdGeneratorImpl(
            ctx = get(),
            analytics = get()
        )
    }.bind(DeviceIdGenerator::class)

    factory {
        object : UUIDGenerator {
            override fun generateUUID(): String = UUID.randomUUID().toString()
        }
    }.bind(UUIDGenerator::class)

    bean {
        PrefsUtil(
            store = PreferenceManager.getDefaultSharedPreferences(/* context = */ get()),
                // TODO where should the constant shared pref name go?
                // TODO how to get the preference object in a cleaner way?
            backupStore = (get() as Context).getSharedPreferences("shared_pref_backup", MODE_PRIVATE),
            idGenerator = get(),
            uuidGenerator = get()
        )
    }.bind(PersistentPrefs::class)
        .bind(CurrencyPrefs::class)
        .bind(NotificationPrefs::class)
        .bind(DashboardPrefs::class)
        .bind(SecurityPrefs::class)
        .bind(ThePitLinkingPrefs::class)
        .bind(SimpleBuyPrefs::class)
        .bind(WalletStatus::class)

    factory { PaymentService(get(), get(), get()) }

    bean {
        if (BuildConfig.DEBUG)
            TimberLogger()
        else
            NullLogger
    }

    factory { EthereumAccountWrapper() }

    bean {
        AccessStateImpl(
            context = get(),
            prefs = get(),
            rxBus = get(),
            crashLogger = get()
        )
    }.bind(AccessState::class)

    factory {
        val accessState = get<AccessState>()
        object : LogoutTimer {
            override fun start() {
                accessState.startLogoutTimer()
            }

            override fun stop() {
                accessState.stopLogoutTimer()
            }
        } as LogoutTimer
    }

    factory { AESUtilWrapper() }
}
