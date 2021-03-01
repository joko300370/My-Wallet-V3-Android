package com.blockchain.koin.modules

import com.blockchain.koin.achDepositWithdrawFeatureFlag
import com.blockchain.koin.achFeatureFlag
import com.blockchain.koin.bankLinkingFeatureFlag
import com.blockchain.koin.dgldFeatureFlag
import com.blockchain.koin.interestAccountFeatureFlag
import com.blockchain.koin.sddFeatureFlag
import com.blockchain.remoteconfig.RemoteConfig
import com.blockchain.remoteconfig.featureFlag
import org.koin.dsl.module

val featureFlagsModule = module {

    factory(interestAccountFeatureFlag) {
        get<RemoteConfig>().featureFlag("interest_account_enabled")
    }

    factory(dgldFeatureFlag) {
        get<RemoteConfig>().featureFlag("wdgld_enabled")
    }

    factory(achFeatureFlag) {
        get<RemoteConfig>().featureFlag("ach_enabled")
    }

    factory(bankLinkingFeatureFlag) {
        get<RemoteConfig>().featureFlag("bank_linking_enabled")
    }
    factory(achDepositWithdrawFeatureFlag) {
        get<RemoteConfig>().featureFlag("ach_deposit_withdrawal_enabled")
    }

    factory(sddFeatureFlag) {
        get<RemoteConfig>().featureFlag("sdd_enabled")
    }
}