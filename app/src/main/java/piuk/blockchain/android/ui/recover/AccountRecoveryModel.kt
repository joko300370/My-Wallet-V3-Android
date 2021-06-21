package piuk.blockchain.android.ui.recover

import com.blockchain.logging.CrashLogger
import info.blockchain.wallet.bip44.HDWalletFactory
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import org.bitcoinj.crypto.MnemonicCode
import org.bitcoinj.crypto.MnemonicException
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import java.util.Locale

class AccountRecoveryModel(
    initialState: AccountRecoveryState,
    mainScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger,
    private val interactor: AccountRecoveryInteractor
) : MviModel<AccountRecoveryState, AccountRecoveryIntents>(
    initialState,
    mainScheduler,
    environmentConfig,
    crashLogger
) {

    private val mnemonicChecker: MnemonicCode by unsafeLazy {
        // We only support US english mnemonics atm
        val wis = HDWalletFactory::class.java.classLoader?.getResourceAsStream(
            "wordlist/" + Locale("en", "US") + ".txt"
        ) ?: throw MnemonicException.MnemonicWordException("cannot read BIP39 word list")

        MnemonicCode(wis, null)
    }

    override fun performAction(previousState: AccountRecoveryState, intent: AccountRecoveryIntents): Disposable? {
        return when (intent) {
            is AccountRecoveryIntents.VerifySeedPhrase -> verifyMnemonic(seedPhrase = intent.seedPhrase)
            is AccountRecoveryIntents.RecoverWalletCredentials -> recoverCredentials(seedPhrase = intent.seedPhrase)
            is AccountRecoveryIntents.RestoreWallet -> restoreWallet()
            is AccountRecoveryIntents.UpdateStatus -> null
        }
    }

    fun verifyMnemonic(seedPhrase: String): Disposable? {
        val seedWords = seedPhrase.trim().split("\\s+".toRegex())
        when {
            seedWords.size < 12 -> {
                process(AccountRecoveryIntents.UpdateStatus(AccountRecoveryStatus.WORD_COUNT_ERROR))
            }
            else -> try {
                mnemonicChecker.check(seedWords)
                process(
                    AccountRecoveryIntents.RecoverWalletCredentials(
                        seedPhrase = seedPhrase
                    )
                )
            } catch (e: MnemonicException) {
                process(AccountRecoveryIntents.UpdateStatus(AccountRecoveryStatus.INVALID_PHRASE))
            }
        }
        return null
    }

    private fun recoverCredentials(seedPhrase: String): Disposable {
        return interactor.recoverCredentials(seedPhrase)
            .subscribeBy(
                onComplete = {
                    process(AccountRecoveryIntents.RestoreWallet)
                },
                onError = {
                    process(AccountRecoveryIntents.UpdateStatus(AccountRecoveryStatus.RECOVERY_FAILED))
                }
            )
    }

    private fun restoreWallet(): Disposable =
        interactor.recoverWallet()
            .subscribeBy(
                onComplete = {
                    process(AccountRecoveryIntents.UpdateStatus(AccountRecoveryStatus.RECOVERY_SUCCESSFUL))
                },
                onError = {
                    process(AccountRecoveryIntents.UpdateStatus(AccountRecoveryStatus.RESET_KYC_FAILED))
                }
            )
}