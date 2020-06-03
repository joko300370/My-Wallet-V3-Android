package piuk.blockchain.android.ui.transfer.send

import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState

enum class SendStep {
    ZERO
}

data class SendState(
    val sendingAccount: CryptoSingleAccount,
    val currentStep: SendStep = SendStep.ZERO
): MviState {

}

class SendModel(
    initialState: SendState,
    mainScheduler: Scheduler,
    private val interactor: SendInteractor
) : MviModel<SendState, SendIntent>(
    initialState,
    mainScheduler
) {
    override fun performAction(previousState: SendState, intent: SendIntent): Disposable? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}