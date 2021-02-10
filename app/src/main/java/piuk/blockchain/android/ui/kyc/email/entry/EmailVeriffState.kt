package piuk.blockchain.android.ui.kyc.email.entry

import android.text.TextUtils
import android.util.Patterns
import piuk.blockchain.android.ui.base.mvi.MviState
import piuk.blockchain.androidcore.data.settings.Email

data class EmailVeriffState(
    val email: Email = Email("", false),
    val emailInput: String = "",
    val isLoading: Boolean = false
) : MviState {

    val canUpdateEmail: Boolean
        get() = emailInputIsValid() && email.address != emailInput

    private fun emailInputIsValid(): Boolean =
        !TextUtils.isEmpty(emailInput) && Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()
}