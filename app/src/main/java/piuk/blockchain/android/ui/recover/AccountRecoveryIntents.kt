package piuk.blockchain.android.ui.recover

import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class AccountRecoveryIntents : MviIntent<AccountRecoveryState> {

    data class VerifySeedPhrase(val seedPhrase: String) : AccountRecoveryIntents() {
        override fun reduce(oldState: AccountRecoveryState): AccountRecoveryState =
            oldState.copy(
                seedPhrase = seedPhrase,
                status = AccountRecoveryStatus.VERIFYING_SEED_PHRASE
            )
    }

    data class UpdateStatus(private val status: AccountRecoveryStatus) : AccountRecoveryIntents() {
        override fun reduce(oldState: AccountRecoveryState): AccountRecoveryState =
            oldState.copy(
                status = status
            )
    }

    data class RecoverWalletCredentials(val seedPhrase: String) : AccountRecoveryIntents() {
        override fun reduce(oldState: AccountRecoveryState): AccountRecoveryState =
            oldState.copy(
                seedPhrase = seedPhrase,
                status = AccountRecoveryStatus.RECOVERING_CREDENTIALS
            )
    }

    object RestoreWallet : AccountRecoveryIntents() {
        override fun reduce(oldState: AccountRecoveryState): AccountRecoveryState =
            oldState.copy(
                status = AccountRecoveryStatus.RECOVERY_SUCCESSFUL
            )
    }
}