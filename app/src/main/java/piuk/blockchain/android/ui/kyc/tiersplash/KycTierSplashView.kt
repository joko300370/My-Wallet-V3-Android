package piuk.blockchain.android.ui.kyc.tiersplash

import androidx.annotation.StringRes
import androidx.navigation.NavDirections
import com.blockchain.swap.nabu.models.responses.nabu.KycTiers

interface KycTierSplashView : piuk.blockchain.androidcoreui.ui.base.View {

    fun navigateTo(directions: NavDirections, tier: Int)

    fun showErrorToast(@StringRes message: Int)

    fun renderTiersList(tiers: KycTiers)
}
