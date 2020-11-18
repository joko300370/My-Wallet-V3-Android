package piuk.blockchain.android.ui.swap

import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.Single

class SwapTypeSwitcher(
    private val newSwapFeatureFlag: FeatureFlag
) {
    fun shouldShowNewSwap(): Single<Boolean> =
        newSwapFeatureFlag.enabled.map { flagEnabled ->
            flagEnabled
        }
}