package piuk.blockchain.android.ui.transactionflow

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.blockchain.featureflags.GatedFeature
import com.blockchain.featureflags.InternalFeatureFlagApi
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.NullCryptoAccount
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.ui.transactionflow.fullscreen.TransactionFlowActivity

class TransactionLauncher(private val flags: InternalFeatureFlagApi, private val context: Context) {

    fun startFlow(
        sourceAccount: BlockchainAccount = NullCryptoAccount(),
        target: TransactionTarget = NullCryptoAccount(),
        action: AssetAction,
        fragmentManager: FragmentManager,
        flowHost: DialogFlow.FlowHost
    ) {
        if (flags.isFeatureEnabled(GatedFeature.FULL_SCREEN_TXS)) {
            context.startActivity(TransactionFlowActivity.newInstance(context, sourceAccount, target, action))
        } else {
            TransactionFlow(sourceAccount, target, action).also {
                it.startFlow(fragmentManager, flowHost)
            }
        }
    }
}