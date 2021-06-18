package piuk.blockchain.android.ui.recover

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

class AccountRecoveryModelTest {

    private lateinit var model: AccountRecoveryModel

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() } `it returns` false
    }

    private val interactor: AccountRecoveryInteractor = mock()

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = AccountRecoveryModel(
            initialState = AccountRecoveryState(),
            mainScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            crashLogger = mock(),
            interactor = interactor
        )
    }

    @Test
    fun `fail to verify short seedphrase should show word count error`() {
        // Arrange
        val seedPhrase = "seed phrase"

        val testState = model.state.test()
        model.process(AccountRecoveryIntents.VerifySeedPhrase(seedPhrase))

        // Assert
        testState.assertValues(
            AccountRecoveryState(),
            AccountRecoveryState(seedPhrase = seedPhrase, status = AccountRecoveryStatus.VERIFYING_SEED_PHRASE),
            AccountRecoveryState(seedPhrase = seedPhrase, status = AccountRecoveryStatus.WORD_COUNT_ERROR)
        )
    }

    @Test
    fun `recover wallet successfully`() {
        // Arrange
        val seedPhrase = "seed phrase seed phrase seed phrase seed phrase seed phrase seed phrase"
        whenever(interactor.recoverCredentials(seedPhrase)).thenReturn(
            Completable.complete()
        )
        whenever(interactor.restoreWallet()).thenReturn(
            Completable.complete()
        )

        val testState = model.state.test()
        model.process(AccountRecoveryIntents.VerifySeedPhrase(seedPhrase))

        // Assert
        testState.assertValues(
            AccountRecoveryState(),
            AccountRecoveryState(seedPhrase = seedPhrase, status = AccountRecoveryStatus.VERIFYING_SEED_PHRASE),
            AccountRecoveryState(seedPhrase = seedPhrase, status = AccountRecoveryStatus.RECOVERING_CREDENTIALS),
            AccountRecoveryState(seedPhrase = seedPhrase, status = AccountRecoveryStatus.RECOVERY_SUCCESSFUL)
        )
    }

    @Test
    fun `fail to recover wallet credentials should show recovery failed`() {
        // Arrange
        val seedPhrase = "seed phrase seed phrase seed phrase seed phrase seed phrase seed phrase"
        whenever(interactor.recoverCredentials(seedPhrase)).thenReturn(
            Completable.error(Exception())
        )

        val testState = model.state.test()
        model.process(AccountRecoveryIntents.VerifySeedPhrase(seedPhrase))

        // Assert
        testState.assertValues(
            AccountRecoveryState(),
            AccountRecoveryState(seedPhrase = seedPhrase, status = AccountRecoveryStatus.VERIFYING_SEED_PHRASE),
            AccountRecoveryState(seedPhrase = seedPhrase, status = AccountRecoveryStatus.RECOVERING_CREDENTIALS),
            AccountRecoveryState(seedPhrase = seedPhrase, status = AccountRecoveryStatus.RECOVERY_FAILED)
        )
    }
}