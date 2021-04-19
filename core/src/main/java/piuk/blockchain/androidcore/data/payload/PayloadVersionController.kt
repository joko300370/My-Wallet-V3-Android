package piuk.blockchain.androidcore.data.payload

import com.blockchain.featureflags.GatedFeature
import com.blockchain.preferences.InternalFeatureFlagPrefs
import info.blockchain.api.WalletSettingsService
import info.blockchain.api.wallet.data.WalletSettingsDto
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import java.util.Locale

internal interface PayloadVersionController {
    fun isV4Enabled(guid: String, sharedKey: String): Single<Boolean>
    // Here as a place holder for when segwit is full rolled out
    val isFullRolloutV4: Boolean get() = false
}

private const val SEGWIT_UPGRADE = "segwit"

// Yes, there is a bit of a hack here.
// But we need to get to the settings _before_ the wallet is decrypted, so we know if to upgrade.
// However, settings are initialised _after_ the payload is loaded and verified - which is conceptually
// correct - so we'll call directly on to the API here, which is nasty but it is what it is and this
// code will be short lived, with luck.
internal class PayloadVersionControllerImpl(
    private val settingsApi: WalletSettingsService,
    private val featureGate: InternalFeatureFlagPrefs
) : PayloadVersionController {

    override fun isV4Enabled(guid: String, sharedKey: String): Single<Boolean> =
        Singles.zip(
            Single.just(featureGate.isFeatureEnabled(GatedFeature.SEGWIT_GLOBAL_ENABLE)),
            Single.just(featureGate.isFeatureEnabled(GatedFeature.SEGWIT_UPGRADE_WALLET)),
            settingsApi.fetchWalletSettings(guid, sharedKey)
        ).map { (globalEnable, upgradeEnable, settings) ->
            val isInvited = settings.isInvitedTo(SEGWIT_UPGRADE)
            globalEnable && (isInvited || upgradeEnable)
        }.onErrorReturn { false }
}

private fun WalletSettingsDto.isInvitedTo(feature: String) =
    feature.toLowerCase(Locale.ROOT).let { feat ->
        invited.filterValues { it }.filterKeys { it.equals(feat, ignoreCase = true) }.isNotEmpty()
    }
