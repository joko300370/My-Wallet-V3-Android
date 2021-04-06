package piuk.blockchain.com

import com.blockchain.featureflags.GatedFeature
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.preferences.InternalFeatureFlagPrefs

class InternalFeatureFlagDebugApiImpl(private val prefs: InternalFeatureFlagPrefs) : InternalFeatureFlagApi {
    override fun isFeatureEnabled(gatedFeature: GatedFeature): Boolean = prefs.isFeatureEnabled(gatedFeature)

    override fun enable(gatedFeature: GatedFeature) = prefs.enableFeature(gatedFeature)

    override fun disable(gatedFeature: GatedFeature) = prefs.disableFeature(gatedFeature)

    override fun disableAll() = prefs.disableAllFeatures()

    override fun getAll(): Map<GatedFeature, Boolean> = prefs.getAllFeatures()
}