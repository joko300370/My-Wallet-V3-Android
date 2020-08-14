package piuk.blockchain.android.ui.transfer.send

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.NullAddress
import piuk.blockchain.android.coincore.NullCryptoAccount
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.SendTarget
import piuk.blockchain.android.coincore.TransactionValidationError
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.ui.base.mvi.MviIntent
import java.util.EmptyStackException

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
                currentStep = if (passwordRequired) {
                    SendStep.ENTER_PASSWORD
                } else {
                    SendStep.ENTER_ADDRESS
                },
                nextEnabled = passwordRequired
            )
    }

    class InitialiseWithTargetAccount(
        val fromAccount: CryptoAccount,
        val toAccount: SendTarget,
        val passwordRequired: Boolean
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState = oldState
    }

    class StartWithDefinedAccounts(
        private val fromAccount: CryptoAccount,
        private val toAccount: SendTarget,
        private val passwordRequired: Boolean,
        private val pendingTx: PendingTx
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            SendState(
                action = AssetAction.Deposit,
                sendingAccount = fromAccount,
                sendTarget = toAccount,
                errorState = SendErrorState.NONE,
                passwordRequired = passwordRequired,
                pendingTx = pendingTx,
                currentStep = if (passwordRequired) {
                    SendStep.ENTER_PASSWORD
                } else {
                    SendStep.ENTER_AMOUNT
                },
                nextEnabled = passwordRequired
            ).updateBackstack(oldState)
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
                currentStep = if (oldState.sendTarget == NullAddress) {
                    SendStep.ENTER_ADDRESS
                } else {
                    SendStep.ENTER_AMOUNT
                }
            ).updateBackstack(oldState)
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
                sendTarget = NullCryptoAccount(),
                nextEnabled = false
            )
    }

    class TargetSelectionConfirmed(
        val sendTarget: SendTarget
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                errorState = SendErrorState.NONE,
                nextEnabled = false
            )
    }

    object FetchFiatRates : SendIntent() {
        override fun reduce(oldState: SendState): SendState = oldState
    }

    object FetchTargetRates : SendIntent() {
        override fun reduce(oldState: SendState): SendState = oldState
    }

    class FiatRateUpdated(
        private val fiatRate: ExchangeRate.CryptoToFiat
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                fiatRate = fiatRate
            )
    }

    class CryptoRateUpdated(
        private val targetRate: ExchangeRate
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                targetRate = targetRate
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
                pendingTx = pendingTx,
                nextEnabled = pendingTx.amount.isPositive
            )
    }

    object PrepareTransaction : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = true,
                currentStep = SendStep.CONFIRM_DETAIL
            ).updateBackstack(oldState)
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
            try {
                val stack = oldState.stepsBackStack
                val previousStep = stack.pop()
                return oldState.copy(
                    stepsBackStack = stack,
                    currentStep = previousStep,
                    errorState = SendErrorState.NONE
                )
            } catch (e: EmptyStackException) {
                // if the stack is empty, throw
                throw IllegalStateException("Cannot go back")
            }
        }
    }

    fun SendState.updateBackstack(oldState: SendState) =
        if (oldState.currentStep != this.currentStep && oldState.currentStep.addToBackStack) {
            val updatedStack = oldState.stepsBackStack
            updatedStack.push(oldState.currentStep)

            this.copy(stepsBackStack = updatedStack)
        } else {
            this
        }
}
