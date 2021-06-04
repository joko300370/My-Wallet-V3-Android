package piuk.blockchain.android.ui.login

import com.blockchain.logging.CrashLogger
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import timber.log.Timber
import javax.net.ssl.SSLPeerUnverifiedException

class LoginModel(
    initialState: LoginState,
    mainScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger,
    private val interactor: LoginInteractor
) : MviModel<LoginState, LoginIntents>(initialState, mainScheduler, environmentConfig, crashLogger) {

    override fun performAction(previousState: LoginState, intent: LoginIntents): Disposable? {
        return when (intent) {
            is LoginIntents.LoginWithQr -> loginWithQrCode(intent.qrString)
            is LoginIntents.SendEmail -> sendVerificationEmail(intent.selectedEmail)
            else -> null
        }
    }

    private fun loginWithQrCode(qrString: String): Disposable {

        return interactor.loginWithQrCode(qrString)
            .subscribeBy(
                onComplete = {
                    process(LoginIntents.StartPinEntry)
                },
                onError = { throwable ->
                    Timber.e(throwable)
                    process(
                        LoginIntents.ShowScanError(
                            shouldRestartApp = throwable is SSLPeerUnverifiedException
                        )
                    )
                }
            )
    }

    private fun sendVerificationEmail(email: String): Disposable {

        return interactor.sendEmailForVerification(email)
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = { process(LoginIntents.ShowEmailSent) },
                onError = { throwable ->
                    Timber.e(throwable)
                    process(LoginIntents.ShowEmailFailed) }
            )
    }
}