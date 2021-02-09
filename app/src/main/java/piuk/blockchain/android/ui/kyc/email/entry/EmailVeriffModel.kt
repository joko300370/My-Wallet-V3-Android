package piuk.blockchain.android.ui.kyc.email.entry

import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.ui.base.mvi.MviModel

class EmailVeriffModel(
    private val interactor: EmailVerifyInteractor,
    observeScheduler: Scheduler
) : MviModel<EmailVeriffState, EmailVeriffIntent>(EmailVeriffState(), observeScheduler) {

    override fun performAction(previousState: EmailVeriffState, intent: EmailVeriffIntent): Disposable? =
        when (intent) {
            EmailVeriffIntent.FetchEmail -> interactor.fetchEmail().subscribeBy(onSuccess = {
                process(EmailVeriffIntent.EmailUpdated(it))
            }, onError = {

            })
            EmailVeriffIntent.CancelEmailVerification -> interactor.cancelPolling().subscribeBy(
                onComplete = {
                },
                onError = {
                }
            )
            EmailVeriffIntent.StartEmailVerification -> interactor.fetchEmail().flatMap {
                if (!it.verified) {
                    interactor.pollForEmailStatus()
                } else Single.just(it)
            }.subscribeBy(onSuccess = {
                process(EmailVeriffIntent.EmailUpdated(it))
            }, onError = {

            })
            else -> null
        }
}