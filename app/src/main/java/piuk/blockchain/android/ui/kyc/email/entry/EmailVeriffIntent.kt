package piuk.blockchain.android.ui.kyc.email.entry

import piuk.blockchain.android.ui.base.mvi.MviIntent
import piuk.blockchain.androidcore.data.settings.Email

sealed class EmailVeriffIntent : MviIntent<EmailVeriffState> {

    class EmailUpdated(private val mail: Email) : EmailVeriffIntent() {
        override fun reduce(oldState: EmailVeriffState): EmailVeriffState = oldState.copy(email = mail)
    }

    object PollForStatus : EmailVeriffIntent() {
        override fun reduce(oldState: EmailVeriffState): EmailVeriffState = oldState
    }

    object FetchEmail : EmailVeriffIntent() {
        override fun reduce(oldState: EmailVeriffState): EmailVeriffState = oldState
    }

    object CancelEmailVerification : EmailVeriffIntent() {
        override fun reduce(oldState: EmailVeriffState): EmailVeriffState = oldState
    }

    object StartEmailVerification : EmailVeriffIntent() {
        override fun reduce(oldState: EmailVeriffState): EmailVeriffState = oldState
    }
}