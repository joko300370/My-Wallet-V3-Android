package com.blockchain.featureflags

interface InternalFeatureFlagApi {
    fun isFeatureEnabled(gatedFeature: GatedFeature): Boolean
    fun enable(gatedFeature: GatedFeature)
    fun disable(gatedFeature: GatedFeature)
    fun disableAll()
    fun getAll(): Map<GatedFeature, Boolean>
}

enum class GatedFeature(val readableName: String, val enabledForCompanyInternalBuild: Boolean = false) {
    ADD_SUB_WALLET_ADDRESSES("Create BTC sub-wallets"),
    SINGLE_SIGN_ON("Enable New SSO Flow"), // If true, the new login flow will be used
    RECURRING_BUYS("Enable recurring buys", true)
}