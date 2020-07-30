package piuk.blockchain.android.ui.dashboard.assetdetails

import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.ui.base.mvi.MviIntent
import piuk.blockchain.android.ui.dashboard.AssetDetailsStep

sealed class AssetDetailsIntent : MviIntent<AssetDetailsState>

class ShowAssetActionsIntent(
    val account: BlockchainAccount,
    val assetFilter: AssetFilter
) : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(
            selectedAccount = account,
            assetDetailsCurrentStep = AssetDetailsStep.ASSET_ACTIONS,
            assetFilter = assetFilter
        )
}

object ShowAssetDetailsIntent : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState =
        oldState.copy(
            assetDetailsCurrentStep = AssetDetailsStep.ASSET_DETAILS
        )
}

object ReturnToPreviousStep : AssetDetailsIntent() {
    override fun reduce(oldState: AssetDetailsState): AssetDetailsState {
        val steps = AssetDetailsStep.values()
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
