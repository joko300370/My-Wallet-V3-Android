package piuk.blockchain.com

import com.blockchain.featureflags.GatedFeature
import com.blockchain.featureflags.InternalFeatureFlagApi

class InternalFeatureFlagApiImpl : InternalFeatureFlagApi {
    override fun isFeatureEnabled(gatedFeature: GatedFeature): Boolean = false

    override fun enable(gatedFeature: GatedFeature) {
        // do nothing
    }

    override fun disable(gatedFeature: GatedFeature) {
        // do nothing
    }

    override fun disableAll() {
        // do nothing
    }

    override fun getAll(): Map<GatedFeature, Boolean> {
        // do nothing
        return mapOf()
    }
}