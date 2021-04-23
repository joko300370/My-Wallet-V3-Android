package com.blockchain.featureflags

interface InternalFeatureFlagApi {
    fun isFeatureEnabled(gatedFeature: GatedFeature): Boolean
    fun enable(gatedFeature: GatedFeature)
    fun disable(gatedFeature: GatedFeature)
    fun disableAll()
    fun getAll(): Map<GatedFeature, Boolean>
}

enum class GatedFeature(val readableName: String) {
    SEGWIT_GLOBAL_ENABLE("Segwit enable"), // If false, segwit totally disabled
    SEGWIT_UPGRADE_WALLET("Segwit upgrade wallet"), // If true, enable upgrade even in not in the get_info cohort
    CHECKOUT("New checkouts")
}