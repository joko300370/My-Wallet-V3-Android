package piuk.blockchain.android.campaign

import com.blockchain.koin.payloadScopeQualifier

import org.koin.dsl.module

val campaignModule = module {

    scope(payloadScopeQualifier) {

        factory {
            SunriverCampaignRegistration(
                nabuDataManager = get(),
                nabuToken = get(),
                kycStatusHelper = get(),
                xlmDataManager = get()
            )
        }
    }
}
