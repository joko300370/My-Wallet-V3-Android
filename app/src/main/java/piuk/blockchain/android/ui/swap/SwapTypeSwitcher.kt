package piuk.blockchain.android.ui.swap

import com.blockchain.preferences.WalletStatus
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.Single
import piuk.blockchain.androidcore.utils.PersistentPrefs

class SwapTypeSwitcher(
    private val walletPrefs: WalletStatus,
    private val newSwapFeatureFlag: FeatureFlag,
    private val prefs: PersistentPrefs
) {
    fun shouldShowNewSwap(): Single<Boolean> =
        newSwapFeatureFlag.enabled.map { flagEnabled ->
            flagEnabled || walletPrefs.isNewUser || prefs.newSwapEnabled
        }
}