package piuk.blockchain.android.ui.login

import piuk.blockchain.android.ui.base.mvi.MviState

enum class LoginStep {
    SELECT_METHOD,
    LOG_IN,
    SHOW_SCAN_ERROR,
    ENTER_PIN,
    ENTER_EMAIL,
    GET_SESSION_ID,
    SEND_EMAIL,
    VERIFY_DEVICE,
    SHOW_SESSION_ERROR,
    SHOW_EMAIL_ERROR
}

data class LoginState(
    val email: String = "",
    val sessionId: String = "",
    val currentStep: LoginStep = LoginStep.SELECT_METHOD,
    val shouldRestartApp: Boolean = false
) : MviState {
    val isLoading: Boolean
        get() = setOf(LoginStep.LOG_IN, LoginStep.GET_SESSION_ID, LoginStep.SEND_EMAIL).contains(currentStep)
    val isTypingEmail: Boolean
        get() = setOf(LoginStep.ENTER_EMAIL, LoginStep.SEND_EMAIL).contains(currentStep)
}
