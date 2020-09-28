package com.blockchain.remoteconfig

import io.reactivex.Single

@Deprecated("We now always use the 'new' algorithm")
class CoinSelectionRemoteConfig(
    private val remoteConfiguration: RemoteConfiguration
) : FeatureFlag {
    override val enabled: Single<Boolean>
        get() = remoteConfiguration.getIfFeatureEnabled("android_use_new_coin_selection")
}
