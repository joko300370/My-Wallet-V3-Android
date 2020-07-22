package piuk.blockchain.android.ui.transfer.send

import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class SendIntent : MviIntent<SendState> {

    class Initialise(
        private val account: CryptoAccount,
        private val passwordRequired: Boolean
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            SendState(
                sendingAccount = account,
                passwordRequired = passwordRequired,
                currentStep = if (passwordRequired) SendStep.ENTER_PASSWORD else SendStep.ENTER_ADDRESS,
                nextEnabled = passwordRequired
            )
    }

    class ValidatePassword(
        val password: String
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = false
            )
    }

    class UpdatePasswordIsValidated(
        val password: String
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = true,
                secondPassword = password,
                currentStep = SendStep.ENTER_ADDRESS
            )
    }

    object UpdatePasswordNotValidated : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = false,
                secondPassword = ""
            )
    }

    class AddressSelected(
        val address: ReceiveAddress
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = true,
                targetAddress = address
            )
    }

    object AddressSelectionConfirmed : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = false,
                currentStep = SendStep.ENTER_AMOUNT
            )
    }

    class SendAmountChanged(
        val amount: CryptoValue
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = false
            )
    }

    object MaxAmountExceeded : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(errorState = SendErrorState.MAX_EXCEEDED)
    }

    object MinRequired : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(errorState = SendErrorState.MIN_REQUIRED)
    }

    object RequestFee : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState
    }

    object FeeRequestError : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(errorState = SendErrorState.FEE_REQUEST_FAILED)
    }

    class FeeUpdate(
        val fee: Money
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(feeAmount = fee)
    }

    object RequestTransactionNoteSupport : SendIntent() {
        override fun reduce(oldState: SendState): SendState = oldState
    }

    object TransactionNoteSupported : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(transactionNoteSupported = true)
    }

    class NoteAdded(
        val note: String
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState = oldState.copy(
            note = note,
            noteState = NoteState.UPDATE_SUCCESS
        )
    }

    class UpdateTransactionAmounts(
        val amount: Money,
        private val maxAvailable: Money
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = amount.isPositive,
                sendAmount = amount,
                availableBalance = maxAvailable,
                errorState = SendErrorState.NONE
            )
    }

    object PrepareTransaction : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = true,
                currentStep = SendStep.CONFIRM_DETAIL
            )
    }

    object ExecuteTransaction : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = false,
                currentStep = SendStep.IN_PROGRESS
            )
    }

    class FatalTransactionError(
        private val error: Throwable
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = true,
                currentStep = SendStep.SEND_ERROR
            )
    }

    object UpdateTransactionComplete : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = true,
                currentStep = SendStep.SEND_COMPLETE
            )
    }

    object ReturnToPreviousStep : SendIntent() {
        override fun reduce(oldState: SendState): SendState {
            val steps = SendStep.values()
            val currentStep = oldState.currentStep.ordinal
            if (currentStep == 0) {
                throw IllegalStateException("Cannot go back")
            }
            val previousStep = steps[currentStep - 1]

            return oldState.copy(
                currentStep = previousStep,
                errorState = SendErrorState.NONE
            )
        }
    }
}
