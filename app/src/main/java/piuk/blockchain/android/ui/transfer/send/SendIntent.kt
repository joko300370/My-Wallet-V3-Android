package piuk.blockchain.android.ui.transfer.send

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.NullAddress
import piuk.blockchain.android.coincore.NullCryptoAccount
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.SendTarget
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.ui.base.mvi.MviIntent
import java.util.EmptyStackException

sealed class SendIntent : MviIntent<SendState> {

    class InitialiseWithSourceAccount(
        private val action: AssetAction,
        private val fromAccount: CryptoAccount,
        private val passwordRequired: Boolean
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            SendState(
                action = action,
                sendingAccount = fromAccount,
                errorState = SendErrorState.NONE,
                passwordRequired = passwordRequired,
                currentStep = selectStep(passwordRequired),
                nextEnabled = passwordRequired
            ).updateBackstack(oldState)

        private fun selectStep(passwordRequired: Boolean): SendStep =
            if (passwordRequired) {
                SendStep.ENTER_PASSWORD
            } else {
                SendStep.ENTER_ADDRESS
            }
    }

    class InitialiseWithSourceAndTargetAccount(
        private val action: AssetAction,
        val fromAccount: CryptoAccount,
        val toAccount: SendTarget,
        private val passwordRequired: Boolean
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            SendState(
                action = action,
                sendingAccount = fromAccount,
                sendTarget = toAccount,
                errorState = SendErrorState.NONE,
                passwordRequired = passwordRequired,
                currentStep = selectStep(passwordRequired),
                nextEnabled = passwordRequired
            ).updateBackstack(oldState)

        private fun selectStep(passwordRequired: Boolean): SendStep =
            if (passwordRequired) {
                SendStep.ENTER_PASSWORD
            } else {
                SendStep.ENTER_AMOUNT
            }
        }

    class ValidatePassword(
        val password: String
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = false,
                errorState = SendErrorState.NONE
            ).updateBackstack(oldState)
    }

    class UpdatePasswordIsValidated(
        val password: String
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = true,
                secondPassword = password,
                currentStep = selectStep(oldState)
            ).updateBackstack(oldState)

        private fun selectStep(oldState: SendState): SendStep =
            if (oldState.sendTarget == NullAddress) {
                SendStep.ENTER_ADDRESS
            } else {
                SendStep.ENTER_AMOUNT
            }
    }

    object UpdatePasswordNotValidated : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = false,
                errorState = SendErrorState.INVALID_PASSWORD,
                secondPassword = ""
            ).updateBackstack(oldState)
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
            ).updateBackstack(oldState)
    }

    class TargetAddressInvalid(private val error: TxValidationFailure) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                errorState = error.state.mapToSendError(),
                sendTarget = NullCryptoAccount(),
                nextEnabled = false
            ).updateBackstack(oldState)
    }

    // When a target is selected and valid, send by the UI to prep the BE for amount input
    class TargetSelectionConfirmed(
        val sendTarget: SendTarget
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                errorState = SendErrorState.NONE,
                sendTarget = sendTarget,
                nextEnabled = false
            ).updateBackstack(oldState)
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
            ).updateBackstack(oldState)
    }

    class CryptoRateUpdated(
        private val targetRate: ExchangeRate
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                targetRate = targetRate
            ).updateBackstack(oldState)
    }

    // Send by the interactor when the transaction engine is started, informs the FE that amount input
    // can be performed and provides any capability flags to the FE
    class PendingTransactionStarted(
        private val canTransactFiat: Boolean
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                errorState = SendErrorState.NONE,
                currentStep = SendStep.ENTER_AMOUNT,
                allowFiatInput = canTransactFiat,
                nextEnabled = false
            ).updateBackstack(oldState)
    }

    class SendAmountChanged(
        val amount: Money
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = false
            ).updateBackstack(oldState)
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
                nextEnabled = pendingTx.validationState == ValidationState.CAN_EXECUTE,
                errorState = pendingTx.validationState.mapToSendError()
            ).updateBackstack(oldState)
    }

    object PrepareTransaction : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = oldState.action != AssetAction.Deposit, // What's this?
                currentStep = SendStep.CONFIRM_DETAIL
            ).updateBackstack(oldState)
    }

    object ExecuteTransaction : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = false,
                currentStep = SendStep.IN_PROGRESS,
                executionStatus = TxExecutionStatus.IN_PROGRESS
            ).updateBackstack(oldState)
    }

    class FatalTransactionError(
        private val error: Throwable
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = true,
                executionStatus = TxExecutionStatus.ERROR
            ).updateBackstack(oldState)
    }

    object UpdateTransactionComplete : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = true,
                executionStatus = TxExecutionStatus.COMPLETED
            ).updateBackstack(oldState)
    }

    // This fn pops the backstack, thus no need to update the backstack here
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

    // When we start the enter amount sheet, we need to kick off a validation pass, so we
    // know to enable the CTA button or not - transactions might require further option
    // setting - t&cs etc - before the Tx can proceed.
    object ValidateTransaction : SendIntent() {
        override fun reduce(oldState: SendState): SendState = oldState
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

private fun ValidationState.mapToSendError() =
    when (this) {
        ValidationState.INVALID_AMOUNT -> SendErrorState.INVALID_AMOUNT
        ValidationState.INSUFFICIENT_FUNDS -> SendErrorState.INSUFFICIENT_FUNDS
        ValidationState.INSUFFICIENT_GAS -> SendErrorState.NOT_ENOUGH_GAS
        ValidationState.CAN_EXECUTE -> SendErrorState.NONE
        ValidationState.UNINITIALISED -> SendErrorState.NONE
        ValidationState.INVALID_ADDRESS -> SendErrorState.INVALID_ADDRESS
        ValidationState.ADDRESS_IS_CONTRACT -> SendErrorState.ADDRESS_IS_CONTRACT
        ValidationState.UNDER_MIN_LIMIT -> SendErrorState.BELOW_MIN_LIMIT
        ValidationState.HAS_TX_IN_FLIGHT, // TODO: Map there better - perhaps add a param for failing txoption?
        ValidationState.OPTION_INVALID,
        ValidationState.OVER_MAX_LIMIT -> SendErrorState.UNEXPECTED_ERROR
    }
