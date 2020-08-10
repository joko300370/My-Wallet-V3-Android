package piuk.blockchain.android.ui.transfer.send

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.NullCryptoAccount
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.SendTarget
import piuk.blockchain.android.coincore.TransactionValidationError
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class SendIntent : MviIntent<SendState> {

    class Initialise(
        private val action: AssetAction,
        private val account: CryptoAccount,
        private val passwordRequired: Boolean
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            SendState(
                action = action,
                sendingAccount = account,
                errorState = SendErrorState.NONE,
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
                nextEnabled = false,
                errorState = SendErrorState.NONE
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
                errorState = SendErrorState.INVALID_PASSWORD,
                secondPassword = ""
            )
    }

    class ValidateInputTargetAddress(
        val targetAddress: String,
        val expectedCrypto: CryptoCurrency
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState = oldState
    }

    class TargetAddressValidated(
        val sendTarget: SendTarget
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                errorState = SendErrorState.NONE,
                sendTarget = sendTarget,
                nextEnabled = true
            )
    }

    class TargetAddressInvalid(private val error: TransactionValidationError) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                errorState = when (error.errorCode) {
                    TransactionValidationError.ADDRESS_IS_CONTRACT -> SendErrorState.ADDRESS_IS_CONTRACT
                    else -> SendErrorState.INVALID_ADDRESS
                },
                sendTarget = NullCryptoAccount,
                nextEnabled = false
            )
        }

    class TargetSelectionConfirmed(
        val sendTarget: SendTarget
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                errorState = SendErrorState.NONE,
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

    class InputValidationError(
        private val error: TransactionValidationError
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                errorState = when (error.errorCode) {
                    TransactionValidationError.INVALID_AMOUNT -> SendErrorState.MIN_REQUIRED
                    TransactionValidationError.INSUFFICIENT_FUNDS -> SendErrorState.MAX_EXCEEDED
                    TransactionValidationError.INSUFFICIENT_GAS -> SendErrorState.NOT_ENOUGH_GAS
                    else -> SendErrorState.UNEXPECTED_ERROR
                }
        )
    }

//    object RequestFee : SendIntent() {
//        override fun reduce(oldState: SendState): SendState =
//            oldState
//    }

//    object FeeRequestError : SendIntent() {
//        override fun reduce(oldState: SendState): SendState =
//            oldState.copy(errorState = SendErrorState.FEE_REQUEST_FAILED)
//    }

//    class FeeUpdate(
//        val fee: CryptoValue
//    ) : SendIntent() {
//        override fun reduce(oldState: SendState): SendState =
//            oldState.copy(feeAmount = fee)
//    }

    class ModifyTxOption(
        val option: TxOptionValue
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState = oldState
    }

    class PendingTxUpdated(
        private val pendingTx: PendingTx
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                pendingTx = pendingTx
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
                currentStep = SendStep.IN_PROGRESS,
                transactionInFlight = TransactionInFlightState.IN_PROGRESS
            )
    }

    class FatalTransactionError(
        private val error: Throwable
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = true,
                transactionInFlight = TransactionInFlightState.ERROR
            )
    }

    object UpdateTransactionComplete : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = true,
                transactionInFlight = TransactionInFlightState.COMPLETED
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
