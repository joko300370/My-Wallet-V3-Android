package piuk.blockchain.android.ui.dashboard.assetdetails

import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState
import piuk.blockchain.android.ui.dashboard.AssetDetailsStep
import timber.log.Timber

data class AssetDetailsState(
    val selectedAccount: BlockchainAccount? = null,
    val assetDetailsCurrentStep: AssetDetailsStep = AssetDetailsStep.ZERO,
    val assetFilter: AssetFilter? = null
) : MviState

class AssetDetailsModel(
    initialState: AssetDetailsState,
    mainScheduler: Scheduler
) : MviModel<AssetDetailsState, AssetDetailsIntent>(initialState, mainScheduler) {
    override fun performAction(
        previousState: AssetDetailsState,
        intent: AssetDetailsIntent
    ): Disposable? {
        Timber.d("***> performAction: ${intent.javaClass.simpleName}")

        return when (intent) {
            is ShowAssetDetailsIntent,
            is ShowAssetActionsIntent,
            is ReturnToPreviousStep -> null
        }
    }

    override fun onScanLoopError(t: Throwable) {
        Timber.e("***> Scan loop failed: $t")
    }
}
