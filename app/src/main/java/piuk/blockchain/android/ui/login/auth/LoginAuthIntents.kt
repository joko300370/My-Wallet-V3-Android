package piuk.blockchain.android.ui.login.auth

import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.HDWalletException
import org.json.JSONObject
import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class LoginAuthIntents : MviIntent<LoginAuthState> {

    data class GetSessionId(val guid: String, val authToken: String) : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                guid = guid,
                authToken = authToken,
                authStatus = AuthStatus.GetSessionId
            )
    }

    data class AuthorizeApproval(val sessionId: String) : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                sessionId = sessionId,
                authStatus = AuthStatus.AuthorizeApproval
            )
    }

    object GetPayload : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                authStatus = AuthStatus.GetPayload
            )
    }

    data class SetPayload(val payloadJson: JSONObject) : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                authMethod = getAuthMethod(oldState),
                payloadJson = payloadJson.toString()
            )

        private fun getAuthMethod(oldState: LoginAuthState): TwoFAMethod {
            return if (payloadJson.isAuth() && (payloadJson.isGoogleAuth() || payloadJson.isSMSAuth())) {
                TwoFAMethod.fromInt(payloadJson.getInt(AUTH_TYPE))
            } else {
                oldState.authMethod
            }
        }
    }

    data class SubmitTwoFactorCode(val password: String, val code: String) : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                password = password,
                code = code,
                authStatus = AuthStatus.Submit2FA
            )
    }

    data class VerifyPassword(val password: String, val payloadJson: String = "") : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                password = password,
                payloadJson = if (payloadJson.isNotEmpty()) payloadJson else oldState.payloadJson,
                authStatus = AuthStatus.VerifyPassword
            )
    }

    object ShowAuthComplete : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                authStatus = AuthStatus.Complete
            )
    }

    object ShowInitialError : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                authStatus = AuthStatus.InitialError
            )
    }

    object ShowAuthRequired : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                authStatus = AuthStatus.AuthRequired
            )
    }

    object Show2FAFailed : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                authStatus = AuthStatus.Invalid2FACode
            )
    }

    data class ShowError(val throwable: Throwable? = null) : LoginAuthIntents() {
        override fun reduce(oldState: LoginAuthState): LoginAuthState =
            oldState.copy(
                authStatus = when (throwable) {
                    is HDWalletException -> AuthStatus.PairingFailed
                    is DecryptionException -> AuthStatus.InvalidPassword
                    else -> AuthStatus.AuthFailed
                }
            )
    }

    companion object {
        const val AUTH_TYPE = "auth_type"
        const val PAYLOAD = "payload"
    }
}

private fun JSONObject.isAuth(): Boolean =
    has(LoginAuthIntents.AUTH_TYPE) && !has(LoginAuthIntents.PAYLOAD)

private fun JSONObject.isGoogleAuth(): Boolean =
    getInt(LoginAuthIntents.AUTH_TYPE) == Settings.AUTH_TYPE_GOOGLE_AUTHENTICATOR

private fun JSONObject.isSMSAuth(): Boolean =
    getInt(LoginAuthIntents.AUTH_TYPE) == Settings.AUTH_TYPE_SMS