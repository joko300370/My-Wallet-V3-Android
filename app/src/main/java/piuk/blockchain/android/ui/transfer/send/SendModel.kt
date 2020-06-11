package piuk.blockchain.android.ui.transfer.send

import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.NullAccount
import piuk.blockchain.android.coincore.NullAddress
import piuk.blockchain.android.coincore.ReceiveAddress
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
    val sendingAccount: CryptoSingleAccount = NullAccount,
    val targetAddress: ReceiveAddress = NullAddress,
    val passwordRequired: Boolean = false,
    val secondPassword: String = "",
    val nextEnabled: Boolean = false
) : MviState

class SendModel(
    initialState: SendState,
    mainScheduler: Scheduler,
    private val interactor: SendInteractor
) : MviModel<SendState, SendIntent>(
    initialState,
    mainScheduler
) {
    override fun performAction(previousState: SendState, intent: SendIntent): Disposable? {
        Timber.d("!SEND!> Send Model: performAction: ${intent.javaClass.simpleName}")

        return when (intent) {
            is SendIntent.Initialise -> null
            is SendIntent.ValidatePassword -> interactor.validatePassword(intent.password)
                .subscribeBy(
                    onSuccess = {
                        process(
                            if (it)
                                SendIntent.UpdatePasswordIsValidated(intent.password)
                            else
                                SendIntent.UpdatePasswordNotValidated)
                    },
                    onError = { /* Error! What to do? Abort? Or... */ }
                )
            is SendIntent.UpdatePasswordIsValidated -> null
            is SendIntent.UpdatePasswordNotValidated -> null
            is SendIntent.AddressSelected -> null
            is SendIntent.PrepareTransaction -> null
            is SendIntent.ExecuteTransaction -> null
            is SendIntent.AddressSelectionConfirmed -> processAddressConfirmation(previousState)
        }
    }

    override fun onScanLoopError(t: Throwable) {
        Timber.d("!SEND!> Send Model: loop error -> $t")
    }

    override fun onStateUpdate(s: SendState) {
        Timber.d("!SEND!> Send Model: state update -> $s")
    }

    private fun processAddressConfirmation(state: SendState): Disposable {
        // We have an address!
        // TODO At this point we can build a transactor object from coincore and configure
        // the state object a bit more; depending on whether it's an internal, external,
        // bitpay or BTC Url address we can set things like note, amount, fee schedule
        // and hook up the correct processor to execute the transaction.

        return Completable.complete().subscribe() // Yeah, just so it builds
    }
}