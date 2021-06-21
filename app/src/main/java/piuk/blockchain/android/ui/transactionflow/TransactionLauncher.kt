package piuk.blockchain.android.ui.transactionflow

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.blockchain.featureflags.GatedFeature
import com.blockchain.featureflags.InternalFeatureFlagApi
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.NullCryptoAccount
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.transactionflow.fullscreen.FullScreenTxFlow

class TransactionLauncher(private val flags: InternalFeatureFlagApi, private val context: Context) {

    fun startFlow(
        sourceAccount: BlockchainAccount = NullCryptoAccount(),
        target: TransactionTarget = NullCryptoAccount(),
        action: AssetAction,
        fragmentManager: FragmentManager,
        flowHost: DialogFlow.FlowHost
    ) = if (flags.isFeatureEnabled(GatedFeature.FULL_SCREEN_TXS)) {
        // TODO
        ToastCustom.makeText(
            context, "Full screen tx flow not available yet", Toast.LENGTH_LONG, ToastCustom.TYPE_ERROR
        )
        FullScreenTxFlow()
    } else {
        TransactionFlow(sourceAccount, target, action).also {
            it.startFlow(fragmentManager, flowHost)
        }
    }
}