package piuk.blockchain.android.ui.transfer.send

import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState
import timber.log.Timber

enum class SendStep {
    ZERO,
    ENTER_PASSWORD,
    ENTER_ADDRESS,
    ENTER_AMOUNT,
    CONFIRM_DETAIL,
    IN_PROGRESS,
    SEND_ERROR,
    SEND_COMPLETE
}

data class SendState(
    val currentStep: SendStep = SendStep.ZERO,
    val sendingAccount: CryptoSingleAccount? = null,
    val passwordRequired: Boolean = false,
    val nextEnabled: Boolean = false
): MviState

class SendModel(
    initialState: SendState,
    mainScheduler: Scheduler,
    private val interactor: SendInteractor
) : MviModel<SendState, SendIntent>(
    initialState,
    mainScheduler
) {
    override fun performAction(previousState: SendState, intent: SendIntent): Disposable? {
        Timber.d("***> Send Model: performAction: ${intent.javaClass.simpleName}")

        return when (intent) {
            is InitialiseWithAccount -> TODO()
            ClearBottomSheet -> null
        }
    }
}