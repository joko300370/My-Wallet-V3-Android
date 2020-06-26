package piuk.blockchain.android.campaign

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.sunriver

import org.koin.dsl.module

val campaignModule = module {

    scope(payloadScopeQualifier) {

        factory {
            SunriverCampaignRegistration(
                featureFlag = get(sunriver),
                nabuDataManager = get(),
                nabuToken = get(),
                kycStatusHelper = get(),
                xlmDataManager = get()
            )
        }
    }
}
