package piuk.blockchain.android.ui.login

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.amshove.kluent.`it returns`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class LoginModelTest {

    private lateinit var model: LoginModel

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() } `it returns` false
    }

    private val interactor: LoginInteractor = mock()

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = LoginModel(
            initialState = LoginState(),
            mainScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            crashLogger = mock(),
            interactor = interactor
        )
    }

    @Test
    fun `pairWithQR success`() {
        // Arrange
        val qrCode = "QR_CODE"
        whenever(interactor.loginWithQrCode(qrCode)).thenReturn(Completable.complete())
        val testState = model.state.test()
        model.process(LoginIntents.LoginWithQr(qrCode))

        // Assert
        testState.assertValues(
            LoginState(),
            LoginState(currentStep = LoginStep.LOG_IN),
            LoginState(currentStep = LoginStep.ENTER_PIN)
        )
    }

    @Test
    fun `pairWithQR fail`() {
        // Arrange
        val qrCode = "QR_CODE"
        whenever(interactor.loginWithQrCode(qrCode)).thenReturn(Completable.error(Throwable()))

        val testState = model.state.test()
        model.process(LoginIntents.LoginWithQr(qrCode))

        // Assert
        testState.assertValues(
            LoginState(),
            LoginState(currentStep = LoginStep.LOG_IN),
            LoginState(currentStep = LoginStep.SHOW_SCAN_ERROR)
        )
    }

    @Test
    fun `enter email manually`() {
        // Arrange
        val email = "test@gmail.com"

        val testState = model.state.test()
        model.process(LoginIntents.UpdateEmail(email))

        // Assert
        testState.assertValues(
            LoginState(),
            LoginState(email = email, currentStep = LoginStep.ENTER_EMAIL)
        )
    }

    @Test
    fun `create session ID and send email successfully`() {
        // Arrange
        val email = "test@gmail.com"
        val sessionId = "sessionId"
        val captcha = "captcha"
        whenever(interactor.obtainSessionId(email)).thenReturn(
            Single.just(
                "{token: $sessionId}".toResponseBody("application/json".toMediaTypeOrNull())
            )
        )
        whenever(interactor.sendEmailForVerification(sessionId, email, captcha)).thenReturn(
            Completable.complete()
        )

        val testState = model.state.test()
        model.process(LoginIntents.ObtainSessionIdForEmail(email, captcha))

        // Assert
        testState.assertValues(
            LoginState(),
            LoginState(email = email, captcha = captcha, currentStep = LoginStep.GET_SESSION_ID),
            LoginState(email = email, sessionId = sessionId, captcha = captcha, currentStep = LoginStep.SEND_EMAIL),
            LoginState(email = email, sessionId = sessionId, captcha = captcha, currentStep = LoginStep.VERIFY_DEVICE)
        )
    }

    @Test
    fun `fail to create session ID`() {
        // Arrange
        val email = "test@gmail.com"
        val captcha = "captcha"
        whenever(interactor.obtainSessionId(email)).thenReturn(
            Single.error(Exception())
        )

        val testState = model.state.test()
        model.process(LoginIntents.ObtainSessionIdForEmail(email, captcha))

        // Assert
        testState.assertValues(
            LoginState(),
            LoginState(email = email, captcha = captcha, currentStep = LoginStep.GET_SESSION_ID),
            LoginState(email = email, captcha = captcha, currentStep = LoginStep.SHOW_SESSION_ERROR)
        )
    }

    @Test
    fun `fail to send email`() {
        // Arrange
        val email = "test@gmail.com"
        val sessionId = "sessionId"
        val captcha = "captcha"
        whenever(interactor.obtainSessionId(email)).thenReturn(
            Single.just(
                "{token: $sessionId}".toResponseBody("application/json".toMediaTypeOrNull())
            )
        )
        whenever(interactor.sendEmailForVerification(sessionId, email, captcha)).thenReturn(
            Completable.error(Throwable())
        )

        val testState = model.state.test()
        model.process(LoginIntents.ObtainSessionIdForEmail(email, captcha))

        // Assert
        testState.assertValues(
            LoginState(),
            LoginState(email = email, captcha = captcha, currentStep = LoginStep.GET_SESSION_ID),
            LoginState(email = email, sessionId = sessionId, captcha = captcha, currentStep = LoginStep.SEND_EMAIL),
            LoginState(
                email = email,
                sessionId = sessionId,
                captcha = captcha,
                currentStep = LoginStep.SHOW_EMAIL_ERROR
            )
        )
    }
}