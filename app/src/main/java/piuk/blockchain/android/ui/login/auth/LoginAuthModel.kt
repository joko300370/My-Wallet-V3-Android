package piuk.blockchain.android.ui.login.auth

import com.blockchain.logging.CrashLogger
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import okhttp3.ResponseBody
import org.json.JSONObject
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import retrofit2.Response

class LoginAuthModel(
    initialState: LoginAuthState,
    mainScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger,
    private val interactor: LoginAuthInteractor
) : MviModel<LoginAuthState, LoginAuthIntents>(initialState, mainScheduler, environmentConfig, crashLogger) {

    override fun performAction(previousState: LoginAuthState, intent: LoginAuthIntents): Disposable? {
        return when (intent) {
            is LoginAuthIntents.GetSessionId -> getSessionId()
            is LoginAuthIntents.AuthorizeApproval ->
                authorizeApproval(
                    authToken = previousState.authToken,
                    sessionId = intent.sessionId
                )
            is LoginAuthIntents.GetPayload -> getPayload(guid = previousState.guid, sessionId = previousState.sessionId)
            is LoginAuthIntents.VerifyPassword ->
                verifyPassword(
                    payload = if (intent.payloadJson.isNotEmpty()) {
                        intent.payloadJson
                    } else {
                        previousState.payloadJson
                    },
                    password = previousState.password
                )
            is LoginAuthIntents.SubmitTwoFactorCode ->
                submitCode(
                    guid = previousState.guid,
                    password = intent.password,
                    sessionId = previousState.sessionId,
                    code = intent.code,
                    payloadJson = previousState.payloadJson
                )
            is LoginAuthIntents.ShowAuthComplete -> clearSessionId()
            else -> null
        }
    }

    private fun getSessionId(): Disposable? {
        process(LoginAuthIntents.AuthorizeApproval(interactor.getSessionId()))
        return null
    }

    private fun clearSessionId(): Disposable? {
        interactor.cleaarSessionId()
        return null
    }

    private fun authorizeApproval(authToken: String, sessionId: String): Disposable {
        return interactor.authorizeApproval(authToken, sessionId)
            .flatMap { response ->
                handleResponse(response)
            }.subscribeBy(
                onSuccess = { process(LoginAuthIntents.GetPayload) },
                onError = { throwable ->
                    process(LoginAuthIntents.ShowError(throwable))
                }
            )
    }

    private fun getPayload(guid: String, sessionId: String): Disposable {
        return interactor.getPayLoad(guid, sessionId)
            .flatMap { response ->
                handleResponse(response)
            }.subscribeBy(
                onSuccess = { responseBody ->
                    val jsonObject = JSONObject(responseBody.string())
                    process(LoginAuthIntents.SetPayload(payloadJson = jsonObject))
                },
                onError = { throwable ->
                    when (throwable) {
                        is InitialErrorException -> process(LoginAuthIntents.ShowInitialError)
                        is AuthRequiredException -> process(LoginAuthIntents.ShowAuthRequired)
                        else -> process(LoginAuthIntents.ShowError(throwable))
                    }
                }
            )
    }

    private fun verifyPassword(payload: String, password: String): Disposable {
        return interactor.verifyPassword(payload, password)
            .subscribeBy(
                onComplete = { process(LoginAuthIntents.ShowAuthComplete) },
                onError = { throwable ->
                    process(LoginAuthIntents.ShowError(throwable))
                }
            )
    }

    private fun submitCode(
        guid: String,
        password: String,
        sessionId: String,
        code: String,
        payloadJson: String
    ): Disposable {
        return interactor.submitCode(guid, sessionId, code, payloadJson)
            .subscribeBy(
                onSuccess = { responseBody ->
                    process(LoginAuthIntents.VerifyPassword(password, responseBody.string()))
                },
                onError = { process(LoginAuthIntents.Show2FAFailed) }
            )
    }

    private fun handleResponse(response: Response<ResponseBody>): Single<ResponseBody> {
        val errorResponse = response.errorBody()
        return if (errorResponse != null && errorResponse.string().isNotEmpty()) {
            val errorBody = JSONObject(errorResponse.string())
            Single.error(
                when {
                    errorBody.has(INITIAL_ERROR) ->
                        InitialErrorException()
                    errorBody.has(KEY_AUTH_REQUIRED) ->
                        AuthRequiredException()
                    else -> UnknownErrorException()
                }
            )
        } else {
            response.body()?.let { responseBody ->
                Single.just(responseBody)
            } ?: Single.error(UnknownErrorException())
        }
    }

    companion object {
        const val INITIAL_ERROR = "initial_error"
        const val KEY_AUTH_REQUIRED = "authorization_required"
    }

    class AuthRequiredException : Exception()
    class InitialErrorException : Exception()
    class UnknownErrorException : Exception()
}