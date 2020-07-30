package com.blockchain.koin.modules

import com.blockchain.koin.cardPaymentsFeatureFlag
import com.blockchain.koin.coinifyFeatureFlag
import com.blockchain.koin.coinifyUsersToKyc
import com.blockchain.koin.interestAccountFeatureFlag
import com.blockchain.koin.pitAnnouncementFeatureFlag
import com.blockchain.koin.pitFeatureFlag
import com.blockchain.koin.simpleBuyFeatureFlag
import com.blockchain.koin.simpleBuyFundsFeatureFlag
import com.blockchain.koin.smsVerifFeatureFlag
import com.blockchain.koin.sunriver
import com.blockchain.remoteconfig.RemoteConfig
import com.blockchain.remoteconfig.featureFlag
import org.koin.dsl.module

val featureFlagsModule = module {
    factory(coinifyUsersToKyc) {
        get<RemoteConfig>().featureFlag("android_notify_coinify_users_to_kyc")
    }

    factory(pitFeatureFlag) {
        get<RemoteConfig>().featureFlag("pit_linking_enabled")
    }

    factory(coinifyFeatureFlag) {
        get<RemoteConfig>().featureFlag("coinify_enabled")
    }

    factory(cardPaymentsFeatureFlag) {
        get<RemoteConfig>().featureFlag("simple_buy_method_card_enabled")
    }

    factory(simpleBuyFundsFeatureFlag) {
        get<RemoteConfig>().featureFlag("simple_buy_method_funds_enabled")
    }

    factory(simpleBuyFeatureFlag) {
        get<RemoteConfig>().featureFlag("simple_buy_enabled")
    }

    factory(pitAnnouncementFeatureFlag) {
        get<RemoteConfig>().featureFlag("pit_show_announcement")
    }

    factory(smsVerifFeatureFlag) {
        get<RemoteConfig>().featureFlag("android_sms_verification")
    }

    factory(sunriver) {
        get<RemoteConfig>().featureFlag("android_sunriver_airdrop_enabled")
    }

    factory(interestAccountFeatureFlag) {
        get<RemoteConfig>().featureFlag("interest_account_enabled")
    }
}