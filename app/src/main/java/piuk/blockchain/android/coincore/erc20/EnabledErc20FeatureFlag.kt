package piuk.blockchain.android.coincore.erc20

import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.Single

class EnabledErc20FeatureFlag(
    override val enabled: Single<Boolean> = Single.just(true)
) : FeatureFlag