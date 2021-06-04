package piuk.blockchain.android.ui.login

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
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
        testState.assertValueAt(0, LoginState())
        testState.assertValueAt(1, LoginState(currentStep = LoginStep.LOG_IN))
        testState.assertValueAt(2, LoginState(currentStep = LoginStep.ENTER_PIN))
    }

    @Test
    fun `pairWithQR fail`() {
        // Arrange
        val qrCode = "QR_CODE"
        whenever(interactor.loginWithQrCode(qrCode)).thenReturn(Completable.error(Throwable()))

        val testState = model.state.test()
        model.process(LoginIntents.LoginWithQr(qrCode))

        // Assert
        testState.assertValueAt(0, LoginState())
        testState.assertValueAt(1, LoginState(currentStep = LoginStep.LOG_IN))
        testState.assertValueAt(2, LoginState(currentStep = LoginStep.SHOW_SCAN_ERROR))
    }

    @Test
    fun `enter email manually`() {
        // Arrange
        val email = "test@gmail.com"

        val testState = model.state.test()
        model.process(LoginIntents.UpdateEmail(email))

        // Assert
        testState.assertValueAt(0, LoginState())
        testState.assertValueAt(1, LoginState(email = email, currentStep = LoginStep.ENTER_EMAIL))
    }

    @Test
    fun `send email successfully`() {
        // Arrange
        val email = "test@gmail.com"
        whenever(interactor.sendEmailForVerification(email)).thenReturn(
            Completable.complete()
        )

        val testState = model.state.test()
        model.process(LoginIntents.SendEmail(email))

        // Assert
        testState.assertValueAt(0, LoginState())
        testState.assertValueAt(1, LoginState(email = email, currentStep = LoginStep.SEND_EMAIL))
        testState.assertValueAt(2, LoginState(email = email, currentStep = LoginStep.VERIFY_DEVICE))
    }

    @Test
    fun `fail to send email`() {
        // Arrange
        val email = "test@gmail.com"
        whenever(interactor.sendEmailForVerification(email)).thenReturn(
            Completable.error(Throwable())
        )

        val testState = model.state.test()
        model.process(LoginIntents.SendEmail(email))

        // Assert
        testState.assertValueAt(0, LoginState())
        testState.assertValueAt(1, LoginState(email = email, currentStep = LoginStep.SEND_EMAIL))
        testState.assertValueAt(2, LoginState(email = email, currentStep = LoginStep.SHOW_EMAIL_ERROR))
    }
}