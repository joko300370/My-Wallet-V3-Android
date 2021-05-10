package com.blockchain.featureflags

interface InternalFeatureFlagApi {
    fun isFeatureEnabled(gatedFeature: GatedFeature): Boolean
    fun enable(gatedFeature: GatedFeature)
    fun disable(gatedFeature: GatedFeature)
    fun disableAll()
    fun getAll(): Map<GatedFeature, Boolean>
}

enum class GatedFeature(val readableName: String) {
    CHECKOUT("New checkouts"),
    MODERN_AUTH_PAIRING("Enable Modern Auth for web login"), // If false, scanning the new web QR won't be processed
    MODERN_AUTH_ENABLE_APPROVAL("Enable Approval for Modern Auth"), // If true, the approve button will be
                                                                    // enabled even if the devices are on different WiFi
    SEGMENT_ANALYTICS("Segment Analytics through Nabu API"),
    ADD_SUB_WALLET_ADDRESSES("Create BTC sub-wallets"),
    SINGLE_SIGN_ON("Enable New SSO Flow") // If true, the new login flow will be used
}