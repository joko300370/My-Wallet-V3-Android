@file:Suppress("USELESS_CAST")
package piuk.blockchain.android.ui.kyc.koin

import piuk.blockchain.android.ui.kyc.splash.KycSplashPresenter
import com.blockchain.sunriver.SunriverCampaignSignUp
import com.blockchain.swap.nabu.CurrentTier
import com.blockchain.swap.nabu.EthEligibility
import com.blockchain.swap.nabu.StartKyc
import com.blockchain.swap.nabu.StartKycForBuySell
import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.ui.kyc.address.CurrentTierAdapter
import piuk.blockchain.android.ui.kyc.address.EligibilityForFreeEthAdapter
import piuk.blockchain.android.ui.kyc.address.KycHomeAddressPresenter
import piuk.blockchain.android.ui.kyc.address.Tier2Decision
import piuk.blockchain.android.ui.kyc.address.Tier2DecisionAdapter
import piuk.blockchain.android.ui.kyc.countryselection.KycCountrySelectionPresenter
import piuk.blockchain.android.ui.kyc.email.entry.KycEmailEntryPresenter
import piuk.blockchain.android.ui.kyc.email.validation.KycEmailValidationPresenter
import piuk.blockchain.android.ui.kyc.invalidcountry.KycInvalidCountryPresenter
import piuk.blockchain.android.ui.kyc.mobile.entry.KycMobileEntryPresenter
import piuk.blockchain.android.ui.kyc.mobile.validation.KycMobileValidationPresenter
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostPresenter
import piuk.blockchain.android.ui.kyc.navhost.KycStarter
import piuk.blockchain.android.ui.kyc.navhost.KycStarterBuySell
import piuk.blockchain.android.ui.kyc.profile.KycProfilePresenter
import piuk.blockchain.android.ui.kyc.reentry.KycNavigator
import piuk.blockchain.android.ui.kyc.reentry.ReentryDecision
import piuk.blockchain.android.ui.kyc.reentry.ReentryDecisionKycNavigator
import piuk.blockchain.android.ui.kyc.reentry.TiersReentryDecision
import piuk.blockchain.android.ui.kyc.status.KycStatusPresenter
import piuk.blockchain.android.ui.kyc.sunriver.SunriverCampaignHelper
import piuk.blockchain.android.ui.kyc.tiersplash.KycTierSplashPresenter
import piuk.blockchain.android.ui.kyc.veriffsplash.VeriffSplashPresenter

val kycUiModule = applicationContext {

    factory { KycStarter() as StartKyc }

    factory { KycStarterBuySell() as StartKycForBuySell }

    factory { TiersReentryDecision() as ReentryDecision }

    context("Payload") {

        factory { ReentryDecisionKycNavigator(get(), get(), get()) as KycNavigator }

        factory {
            KycTierSplashPresenter(
                get(),
                get(),
                get(),
                get("ff_sunriver_has_large_backlog")
            )
        }

        factory { KycSplashPresenter(get(), get(), get(), get(), get()) }

        factory { KycCountrySelectionPresenter(get(), get()) }

        factory {
            KycProfilePresenter(nabuToken = get(),
                nabuDataManager = get(),
                metadataRepository = get(),
                stringUtils = get())
        }

        factory {
            KycHomeAddressPresenter(
                nabuToken = get(),
                nabuDataManager = get(),
                tier2Decision = get(),
                phoneVerificationQuery = get(),
                nabuCoinifyAccountCreator = get())
        }

        factory { KycMobileEntryPresenter(get(), get()) }

        factory { KycMobileValidationPresenter(get(), get()) }

        factory { KycEmailEntryPresenter(get()) }

        factory { KycEmailValidationPresenter(get(), get()) }

        factory {
            VeriffSplashPresenter(
                nabuToken = get(),
                nabuDataManager = get(),
                analytics = get(),
                prefs = get()
            )
        }

        factory { KycStatusPresenter(get(), get(), get()) }

        factory { KycNavHostPresenter(get(), get(), get(), get(), get(), get(), get()) }

        factory { KycInvalidCountryPresenter(get(), get()) }
    }
}

val kycUiNabuModule = applicationContext {

    context("Payload") {

        factory {
            Tier2DecisionAdapter(get(), get()) as Tier2Decision
        }

        factory {
            CurrentTierAdapter(get(), get()) as CurrentTier
        }

        factory {
            EligibilityForFreeEthAdapter(
                nabuToken = get(),
                nabuDataManager = get()
            ) as EthEligibility
        }

        factory { SunriverCampaignHelper(get("sunriver"), get(), get(), get(), get()) }
            .bind(SunriverCampaignSignUp::class)
    }
}
