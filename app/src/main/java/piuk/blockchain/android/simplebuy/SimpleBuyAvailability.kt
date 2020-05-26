package piuk.blockchain.android.simplebuy

import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.Single

class SimpleBuyAvailability(
    private val simpleBuyFlag: FeatureFlag
) {
    fun isAvailable(): Single<Boolean> {
        return simpleBuyFlag.enabled
    }
}