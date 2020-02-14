package com.blockchain.koin

import com.blockchain.swap.nabu.Authenticator
import com.blockchain.swap.nabu.CreateNabuToken
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.NabuUserSync
import com.blockchain.swap.nabu.api.nabu.Nabu
import com.blockchain.swap.nabu.api.nabu.NabuMarkets
import com.blockchain.swap.nabu.api.trade.TransactionStateAdapter
import com.blockchain.swap.nabu.datamanagers.AnalyticsNabuUserReporterImpl
import com.blockchain.swap.nabu.datamanagers.AnalyticsWalletReporter
import com.blockchain.swap.nabu.datamanagers.CreateNabuTokenAdapter
import com.blockchain.swap.nabu.datamanagers.custodialwalletimpl.CustodialWalletManagerSwitcher
import com.blockchain.swap.nabu.datamanagers.custodialwalletimpl.LiveCustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.custodialwalletimpl.StubCustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.NabuAuthenticator
import com.blockchain.swap.nabu.datamanagers.NabuDataManager
import com.blockchain.swap.nabu.datamanagers.NabuDataManagerImpl
import com.blockchain.swap.nabu.datamanagers.NabuDataUserProvider
import com.blockchain.swap.nabu.datamanagers.NabuDataUserProviderNabuDataManagerAdapter
import com.blockchain.swap.nabu.datamanagers.NabuUserReporter
import com.blockchain.swap.nabu.datamanagers.NabuUserSyncUpdateUserWalletInfoWithJWT
import com.blockchain.swap.nabu.datamanagers.UniqueAnalyticsNabuUserReporter
import com.blockchain.swap.nabu.datamanagers.UniqueAnalyticsWalletReporter
import com.blockchain.swap.nabu.datamanagers.WalletReporter
import com.blockchain.swap.nabu.metadata.MetadataRepositoryNabuTokenAdapter
import com.blockchain.swap.nabu.models.nabu.CampaignStateMoshiAdapter
import com.blockchain.swap.nabu.models.nabu.CampaignTransactionStateMoshiAdapter
import com.blockchain.swap.nabu.models.nabu.IsoDateMoshiAdapter
import com.blockchain.swap.nabu.models.nabu.KycStateAdapter
import com.blockchain.swap.nabu.models.nabu.KycTierStateAdapter
import com.blockchain.swap.nabu.models.nabu.UserCampaignStateMoshiAdapter
import com.blockchain.swap.nabu.models.nabu.UserStateAdapter
import com.blockchain.swap.nabu.service.NabuCoinifyAccountCreator
import com.blockchain.swap.nabu.service.NabuCoinifyAccountService
import com.blockchain.swap.nabu.service.NabuMarketsService
import com.blockchain.swap.nabu.service.NabuService
import com.blockchain.swap.nabu.service.NabuTierService
import com.blockchain.swap.nabu.service.RetailWalletTokenService
import com.blockchain.swap.nabu.service.TierService
import com.blockchain.swap.nabu.service.TierUpdater
import com.blockchain.swap.nabu.service.TradeLimitService
import com.blockchain.swap.nabu.status.KycTiersQueries
import com.blockchain.swap.nabu.stores.NabuSessionTokenStore
import org.koin.dsl.module.applicationContext
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.repositories.AccessTokenStore
import piuk.blockchain.androidbuysell.services.CoinifyService
import retrofit2.Retrofit

val nabuModule = applicationContext {

    bean { get<Retrofit>("nabu").create(NabuMarkets::class.java) }

    context("Payload") {

        factory { NabuMarketsService(get(), get()) }
            .bind(TradeLimitService::class)

        factory {
            MetadataRepositoryNabuTokenAdapter(
                metadataRepository = get(),
                createNabuToken = get(),
                metadataManager = get()
            )
        }.bind(NabuToken::class)

        factory {
            NabuDataManagerImpl(
                nabuService = get(),
                retailWalletTokenService = get(),
                nabuTokenStore = get(),
                appVersion = getProperty("app-version"),
                settingsDataManager = get(),
                payloadDataManager = get(),
                prefs = get(),
                walletReporter = get("unique_id"),
                userReporter = get("unique_user_analytics")
            ) as NabuDataManager
        }

        factory {
            CustodialWalletManagerSwitcher(
                mockCustodialWalletManager = StubCustodialWalletManager(),
                liveCustodialWalletManager = LiveCustodialWalletManager(
                    nabuService = get(),
                    authenticator = get(),
                    paymentAccountMapperMappers = mapOf(
                        "EUR" to get("EUR"), "GBP" to get("GBP")
                    )
                )
            )
        }.bind(CustodialWalletManager::class)

        factory("unique_user_analytics") {
            UniqueAnalyticsNabuUserReporter(
                nabuUserReporter = get("user_analytics"),
                prefs = get()
            )
        }.bind(NabuUserReporter::class)

        factory("user_analytics") {
            AnalyticsNabuUserReporterImpl(
                userAnalytics = get()
            )
        }.bind(NabuUserReporter::class)

        factory("unique_id") {
            UniqueAnalyticsWalletReporter(get("wallet_analytics"), prefs = get())
        }.bind(WalletReporter::class)

        factory("wallet_analytics") {
            AnalyticsWalletReporter(userAnalytics = get())
        }.bind(WalletReporter::class)

        factory {
            NabuCoinifyAccountService(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get()
            ) as NabuCoinifyAccountCreator
        }

        factory {
            get<Retrofit>("nabu").create(Nabu::class.java)
        }

        factory { NabuTierService(get(), get()) }
            .bind(TierService::class)
            .bind(TierUpdater::class)

        factory {
            CreateNabuTokenAdapter(get()) as CreateNabuToken
        }
    }

    moshiInterceptor("nabu") { builder ->
        builder.add(TransactionStateAdapter())
    }

    bean { NabuSessionTokenStore() }

    bean { NabuService(get("nabu")) }

    bean { RetailWalletTokenService(get(), getProperty("api-code"), get("kotlin")) }

    context("Payload") {

        factory { NabuDataUserProviderNabuDataManagerAdapter(get(), get()) as NabuDataUserProvider }

        factory { NabuUserSyncUpdateUserWalletInfoWithJWT(get(), get()) as NabuUserSync }

        factory { KycTiersQueries(get(), get()) }
    }

    moshiInterceptor("kyc") { builder ->
        builder
            .add(KycStateAdapter())
            .add(KycTierStateAdapter())
            .add(UserStateAdapter())
            .add(IsoDateMoshiAdapter())
            .add(UserCampaignStateMoshiAdapter())
            .add(CampaignStateMoshiAdapter())
            .add(CampaignTransactionStateMoshiAdapter())
    }
}

val coinifyModule = applicationContext {

    context("Payload") {

        bean { AccessTokenStore() }

        factory { CoinifyDataManager(get(), get(), get()) }

        factory { CoinifyService(get(), get("kotlin"), get()) }
    }
}

val authenticationModule = applicationContext {
    context("Payload") {
        factory {
            NabuAuthenticator(get(), get()) as Authenticator
        }
    }
}