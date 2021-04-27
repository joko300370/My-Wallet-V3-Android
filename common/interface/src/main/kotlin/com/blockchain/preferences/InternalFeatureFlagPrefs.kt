package com.blockchain.preferences

import com.blockchain.featureflags.GatedFeature

interface InternalFeatureFlagPrefs {
    fun isFeatureEnabled(gatedFeature: GatedFeature): Boolean
    fun enableFeature(gatedFeature: GatedFeature)
    fun disableFeature(gatedFeature: GatedFeature)
    fun disableAllFeatures()
    fun getAllFeatures(): Map<GatedFeature, Boolean>
}