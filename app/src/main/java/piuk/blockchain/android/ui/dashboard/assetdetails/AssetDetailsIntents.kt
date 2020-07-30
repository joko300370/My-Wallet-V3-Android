package piuk.blockchain.android.ui.dashboard.assetdetails

import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.ui.base.mvi.MviIntent
import piuk.blockchain.android.ui.dashboard.DashboardStep

sealed class AssetDetailsIntent : MviIntent<AssetDetailsState>

class ShowAssetActionsIntent(
    val account: BlockchainAccount
) : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(
            selectedAccount = account,
            assetDetailsCurrentStep = DashboardStep.ASSET_ACTIONS
        )
}

object ShowAssetDetailsIntent : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(
            assetDetailsCurrentStep = DashboardStep.ASSET_DETAILS
        )
}

object ReturnToPreviousStep : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState {
        val steps = DashboardStep.values()
        val currentStep = oldState.assetDetailsCurrentStep.ordinal
        if (currentStep == 0) {
            throw IllegalStateException("Cannot go back")
        }
        val previousStep = steps[currentStep - 1]

        return oldState.copy(
            assetDetailsCurrentStep = previousStep
        )
    }
}
