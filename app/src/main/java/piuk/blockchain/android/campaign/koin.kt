package piuk.blockchain.android.campaign

import org.koin.dsl.module.applicationContext

val campaignModule = applicationContext {

    context("Payload") {

        factory {
            SunriverCampaignRegistration(
                featureFlag = get("sunriver"),
                nabuDataManager = get(),
                nabuToken = get(),
                kycStatusHelper = get(),
                xlmDataManager = get()
            )
        }

        factory {
            BlockstackCampaignRegistration(
                nabuDataManager = get(),
                nabuToken = get(),
                stx = get()
            )
        }
    }
}
