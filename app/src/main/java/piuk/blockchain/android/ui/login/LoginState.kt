package piuk.blockchain.android.ui.login

import piuk.blockchain.android.ui.base.mvi.MviState

enum class LoginStep {
    SELECT_METHOD,
    LOG_IN,
    SHOW_SCAN_ERROR,
    ENTER_PIN,
    ENTER_EMAIL,
    SEND_EMAIL,
    VERIFY_DEVICE,
    SHOW_EMAIL_ERROR
}

data class LoginState(
    val email: String = "",
    val currentStep: LoginStep = LoginStep.SELECT_METHOD,
    val shouldRestartApp: Boolean = false
) : MviState {
    val isLoggingIn: Boolean
        get() = currentStep == LoginStep.LOG_IN
    val isTypingEmail: Boolean
        get() = currentStep == LoginStep.ENTER_EMAIL
}
