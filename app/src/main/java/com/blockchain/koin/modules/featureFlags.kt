package com.blockchain.koin.modules

import com.blockchain.koin.aaveFeatureFlag
import com.blockchain.koin.achDepositWithdrawFeatureFlag
import com.blockchain.koin.achFeatureFlag
import com.blockchain.koin.bankLinkingFeatureFlag
import com.blockchain.koin.dgldFeatureFlag
import com.blockchain.koin.dotFeatureFlag
import com.blockchain.koin.interestAccountFeatureFlag
import com.blockchain.koin.sddFeatureFlag
import com.blockchain.koin.yfiFeatureFlag
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

    factory(aaveFeatureFlag) {
        get<RemoteConfig>().featureFlag("aave_enabled")
    }

    factory(yfiFeatureFlag) {
        get<RemoteConfig>().featureFlag("yfi_enabled")
    }

    factory(dotFeatureFlag) {
        get<RemoteConfig>().featureFlag("dot_enabled")
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

/*
val localFeatureFlag = module {
    LFeatureFlag.values().forEach { ff ->
        factory(ff.qualifier()) {
            PrefsLocalFeatureFlag(get(), false)
        }.bind(FlaggedFeature::class)
    }
}

class PrefsLocalFeatureFlag(private val persistance: LocalFeatureFlagPersistence,private val name:LFeatureFlag, private val defValue: Boolean) :
    LocalFeatureFlag {
    override fun isEnabled(): Boolean {
        return persistance.enabled(defValue,name.toString())
    }
}

interface LocalFeatureFlagPersistence {
    fun enabled(defValue: Boolean, name: String): Boolean
    fun update(defValue: Boolean, name: String): Boolean
    fun (defValue: Boolean, name: String): Boolean
}

interface FlaggedFeature {
    fun isEnabled(): Boolean
}

class PrefsFeatureFlagPers(private val prefs: PersistentPrefs) : LocalFeatureFlagPersistence {

    override fun enabled(defValue: Boolean, name: String): Boolean = prefs.getValue(name, defValue)
    override fun update(defValue: Boolean, name: String): Boolean = prefs.getValue(name, defValue)
}

enum class LFeatureFlag() {
    OPEN_BANKING(false), ACH(false), SEGWIT(false);

    fun qualifier(): StringQualifier = StringQualifier(this.toString())
}*/
