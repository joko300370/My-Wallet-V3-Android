package piuk.blockchain.android.ui.transfer.send

import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class SendIntent(

) : MviIntent<SendState>{

}

object ClearBottomSheet : SendIntent() {
    override fun reduce(oldState: SendState): SendState =
        oldState.copy(currentStep = SendStep.ZERO)
}