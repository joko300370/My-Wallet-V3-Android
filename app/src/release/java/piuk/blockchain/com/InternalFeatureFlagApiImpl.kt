package piuk.blockchain.com

import com.blockchain.nabu.datamanagers.featureflags.InternalFeatureFlagApi
import com.blockchain.preferences.Feature

class InternalFeatureFlagApiImpl() : InternalFeatureFlagApi {
    override fun isEnabled(feature: Feature): Boolean = false

    override fun enable(feature: Feature) {
        // do nothing
    }

    override fun disable(feature: Feature) {
        // do nothing
    }

    override fun disableAll() {
        // do nothing
    }

    override fun getAll(): Map<Feature, Boolean> {
        // do nothing
        return mapOf()
    }
}