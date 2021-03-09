package piuk.blockchain.android.ui.kyc.email

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.amshove.kluent.`it returns`
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.kyc.email.entry.EmailVeriffIntent
import piuk.blockchain.android.ui.kyc.email.entry.EmailVeriffModel
import piuk.blockchain.android.ui.kyc.email.entry.EmailVeriffState
import piuk.blockchain.android.ui.kyc.email.entry.EmailVerifyInteractor
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.settings.Email

class EmailVeriffModelTest {

    private val interactor: EmailVerifyInteractor = mock()

    private lateinit var model: EmailVeriffModel

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() } `it returns` false
    }

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = EmailVeriffModel(
            interactor = interactor,
            observeScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            crashLogger = mock()
        )
    }

    @Test
    fun `for unverified email, it should return the unverified email and then the polling result`() {
        whenever(interactor.cancelPolling()).thenReturn(Completable.complete())
        whenever(interactor.fetchEmail()).thenReturn(Single.just(Email("address@example.com", false)))
        whenever(interactor.pollForEmailStatus()).thenReturn(Single.just(Email("address@example.com", true)))

        val statesTest = model.state.test()
        model.process(EmailVeriffIntent.StartEmailVerification)

        statesTest.assertValueAt(0, EmailVeriffState())
        statesTest.assertValueAt(
            1, EmailVeriffState(
                email = Email("address@example.com", false)
            )
        )
        statesTest.assertValueAt(
            2, EmailVeriffState(
                email = Email("address@example.com", true)
            )
        )
    }
}