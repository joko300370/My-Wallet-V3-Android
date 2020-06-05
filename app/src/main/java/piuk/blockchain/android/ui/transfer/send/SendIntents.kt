package piuk.blockchain.android.ui.transfer.send

import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class SendIntent(

) : MviIntent<SendState>{

}

class InitialiseWithAccount(
    val account: CryptoSingleAccount,
    val passwordRequired: Boolean
): SendIntent() {
    override fun reduce(oldState: SendState): SendState =
        SendState(
            sendingAccount = account,
            passwordRequired = passwordRequired
        )
}

object ClearBottomSheet : SendIntent() {
    override fun reduce(oldState: SendState): SendState =
        oldState.copy(currentStep = SendStep.ZERO)
}