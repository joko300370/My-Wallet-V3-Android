package piuk.blockchain.android.ui.transfer.send

import info.blockchain.balance.CryptoValue
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class SendIntent : MviIntent<SendState> {

    class Initialise(
        val account: CryptoSingleAccount,
        val passwordRequired: Boolean
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            SendState(
                sendingAccount = account,
                passwordRequired = passwordRequired,
                currentStep = if (passwordRequired) SendStep.ENTER_PASSWORD else SendStep.ENTER_ADDRESS,
                nextEnabled = passwordRequired,
                processing = false
            )
        }

    class ValidatePassword(
        val password: String
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = false,
                processing = true
            )
        }

    class UpdatePasswordIsValidated(
        val password: String
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = false,
                processing = true,
                secondPassword = password,
                currentStep = SendStep.ENTER_ADDRESS
            )
        }

    object UpdatePasswordNotValidated : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = true,
                processing = false,
                secondPassword = ""
            )
        }

    class AddressSelected(
        val address: CryptoAddress
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = false,
                processing = false,
                currentStep = SendStep.ENTER_AMOUNT
            )
    }

    class PrepareTransaction(
        val amount: CryptoValue
    ) : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = false,
                processing = false,
                currentStep = SendStep.CONFIRM_DETAIL
            )
    }

    object ExecuteTransaction : SendIntent() {
        override fun reduce(oldState: SendState): SendState =
            oldState.copy(
                nextEnabled = false,
                processing = false,
                currentStep = SendStep.IN_PROGRESS
            )
    }
}
