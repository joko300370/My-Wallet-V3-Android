package com.blockchain.featureflags

interface InternalFeatureFlagApi {
    fun isFeatureEnabled(gatedFeature: GatedFeature): Boolean
    fun enable(gatedFeature: GatedFeature)
    fun disable(gatedFeature: GatedFeature)
    fun disableAll()
    fun getAll(): Map<GatedFeature, Boolean>
}

enum class GatedFeature(val readableName: String) {
    OB_SB_SETT("OB Simple buy & Settings"),
    SEGWIT("Segwit"),
    OB_DEPO_WITH("OB Deposit & Withdraw")
}