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
    CHECKOUT("New checkouts"),
    MODERN_AUTH_PAIRING("Enable Modern Auth for web login"), // If false, scanning the new web QR won't be processed
    MODERN_AUTH_ENABLE_APPROVAL("Enable Approval for Modern Auth"), // If true, the approve button will be
                                                                    // enabled even if the devices are on different WiFi
    SEGMENT_ANALYTICS("Segment Analytics through Nabu API"),
    ADD_SUB_WALLET_ADDRESSES("Create BTC sub-wallets")
}