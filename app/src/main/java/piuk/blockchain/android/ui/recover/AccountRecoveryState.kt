package piuk.blockchain.android.ui.recover

import piuk.blockchain.android.ui.base.mvi.MviState

enum class AccountRecoveryStatus {
    INIT,
    VERIFYING_SEED_PHRASE,
    INVALID_PHRASE,
    WORD_COUNT_ERROR,
    RECOVERING_CREDENTIALS,
    RECOVERY_SUCCESSFUL,
    RECOVERY_FAILED
}

data class AccountRecoveryState(
    val seedPhrase: String = "",
    val status: AccountRecoveryStatus = AccountRecoveryStatus.INIT
) : MviState