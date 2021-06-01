package piuk.blockchain.android.ui.login.auth

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.exceptions.DecryptionException
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.amshove.kluent.`it returns`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import retrofit2.Response

class LoginAuthModelTest {
    private lateinit var model: LoginAuthModel

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() } `it returns` false
    }

    private val interactor: LoginAuthInteractor = mock()

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = LoginAuthModel(
            initialState = LoginAuthState(),
            mainScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            crashLogger = mock(),
            interactor = interactor
        )
    }

    @Test
    fun `get session ID and payload`() {
        // Arrange
        val sessionId = "SESSION_ID"
        val guid = "GUID"
        val authToken = "TOKEN"
        val responseBody = EMPTY_RESPONSE.toResponseBody(JSON_HEADER.toMediaTypeOrNull())
        whenever(interactor.getSessionId()).thenReturn(sessionId)
        whenever(interactor.authorizeApproval(authToken, sessionId)).thenReturn(
            Single.just(Response.success(responseBody))
        )
        whenever(interactor.getPayLoad(anyString(), anyString())).thenReturn(
            Single.just(Response.success(responseBody))
        )

        val testState = model.state.test()
        model.process(LoginAuthIntents.GetSessionId(guid, authToken))

        // Assert
        testState.assertValues(
            LoginAuthState(),
            LoginAuthState(guid = guid, authToken = authToken, authStatus = AuthStatus.GetSessionId),
            LoginAuthState(
                guid = guid,
                sessionId = sessionId,
                authToken = authToken,
                authStatus = AuthStatus.AuthorizeApproval
            ),
            LoginAuthState(
                guid = guid,
                sessionId = sessionId,
                authToken = authToken,
                authStatus = AuthStatus.GetPayload
            ),
            LoginAuthState(
                guid = guid,
                sessionId = sessionId,
                authToken = authToken,
                authStatus = AuthStatus.GetPayload,
                payloadJson = EMPTY_RESPONSE
            )
        )
    }

    @Test
    fun `auth fail to get payload`() {
        // Arrange
        val sessionId = "SESSION_ID"
        val guid = "GUID"
        val authToken = "TOKEN"
        val responseBody = EMPTY_RESPONSE.toResponseBody(JSON_HEADER.toMediaTypeOrNull())
        whenever(interactor.getSessionId()).thenReturn(sessionId)
        whenever(interactor.authorizeApproval(authToken, sessionId)).thenReturn(
            Single.just(Response.success(responseBody))
        )
        whenever(interactor.getPayLoad(guid, sessionId)).thenReturn(Single.error(Exception()))

        val testState = model.state.test()
        model.process(LoginAuthIntents.GetSessionId(guid, authToken))

        // Assert
        testState.assertValues(
            LoginAuthState(),
            LoginAuthState(guid = guid, authToken = authToken, authStatus = AuthStatus.GetSessionId),
            LoginAuthState(
                guid = guid,
                sessionId = sessionId,
                authToken = authToken,
                authStatus = AuthStatus.AuthorizeApproval
            ),
            LoginAuthState(
                guid = guid,
                sessionId = sessionId,
                authToken = authToken,
                authStatus = AuthStatus.GetPayload
            ),
            LoginAuthState(
                guid = guid,
                sessionId = sessionId,
                authToken = authToken,
                authStatus = AuthStatus.AuthFailed
            )
        )
    }

    @Test
    fun `verify password without 2fa`() {
        // Arrange
        val password = "password"
        whenever(interactor.verifyPassword(anyString(), anyString())).thenReturn(
            Completable.complete()
        )

        val testState = model.state.test()
        model.process(LoginAuthIntents.VerifyPassword(password))

        // Assert
        testState.assertValues(
            LoginAuthState(),
            LoginAuthState(
                authStatus = AuthStatus.VerifyPassword,
                password = password
            ),
            LoginAuthState(
                authStatus = AuthStatus.Complete,
                password = password
            )
        )
    }

    @Test
    fun `fail to verify password`() {
        // Arrange
        val password = "password"
        whenever(interactor.verifyPassword(anyString(), anyString())).thenReturn(
            Completable.error(
                DecryptionException()
            )
        )

        val testState = model.state.test()
        model.process(LoginAuthIntents.VerifyPassword(password))

        // Assert
        testState.assertValues(
            LoginAuthState(),
            LoginAuthState(
                authStatus = AuthStatus.VerifyPassword,
                password = password
            ),
            LoginAuthState(
                authStatus = AuthStatus.InvalidPassword,
                password = password
            )
        )
    }

    @Test
    fun `verify password with 2fa`() {
        // Arrange
        val password = "password"
        val twoFACode = "code"
        whenever(interactor.submitCode(anyString(), anyString(), anyString(), anyString())).thenReturn(
            Single.just(TWO_FA_PAYLOAD.toResponseBody((JSON_HEADER).toMediaTypeOrNull()))
        )
        whenever(interactor.verifyPassword(anyString(), anyString())).thenReturn(
            Completable.complete()
        )

        val testState = model.state.test()
        model.process(LoginAuthIntents.SubmitTwoFactorCode(password, twoFACode))

        // Assert
        testState.assertValues(
            LoginAuthState(),
            LoginAuthState(
                authStatus = AuthStatus.Submit2FA,
                password = password,
                code = twoFACode
            ),
            LoginAuthState(
                authStatus = AuthStatus.VerifyPassword,
                password = password,
                code = twoFACode,
                payloadJson = TWO_FA_PAYLOAD
            ),
            LoginAuthState(
                authStatus = AuthStatus.Complete,
                password = password,
                code = twoFACode,
                payloadJson = TWO_FA_PAYLOAD
            )
        )
    }

    @Test
    fun `fail to verify 2fa`() {
        val password = "password"
        val twoFACode = "code"
        whenever(interactor.submitCode(anyString(), anyString(), anyString(), anyString())).thenReturn(
            Single.error(Exception())
        )

        val testState = model.state.test()
        model.process(LoginAuthIntents.SubmitTwoFactorCode(password, twoFACode))

        // Assert
        testState.assertValues(
            LoginAuthState(),
            LoginAuthState(
                authStatus = AuthStatus.Submit2FA,
                password = password,
                code = twoFACode
            ),
            LoginAuthState(
                authStatus = AuthStatus.Invalid2FACode,
                password = password,
                code = twoFACode
            )
        )
    }

    companion object {

        private const val EMPTY_RESPONSE = "{}"

        private const val JSON_HEADER = "application/json"

        private const val INITIAL_ERROR_MESSAGE = "This is an error"

        private const val INITIAL_ERROR_RESPONSE = "{\"initial_error\":\"$INITIAL_ERROR_MESSAGE\"}"

        private const val TWO_FA_PAYLOAD = "{\"payload\":\"{auth_type: 4}\"}"
    }
}