package piuk.blockchain.android.ui.kyc.email.entry

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.ui.base.mvi.MviModel

class EmailVeriffModel(
    private val interactor: EmailVerifyInteractor,
    observeScheduler: Scheduler
) : MviModel<EmailVeriffState, EmailVeriffIntent>(EmailVeriffState(), observeScheduler) {

    override fun performAction(previousState: EmailVeriffState, intent: EmailVeriffIntent): Disposable? =
        when (intent) {
            EmailVeriffIntent.FetchEmail -> interactor.fetchEmail().subscribeBy(
                onSuccess = {
                    process(EmailVeriffIntent.EmailUpdated(it))
                }, onError = {}
            )
            EmailVeriffIntent.CancelEmailVerification -> interactor.cancelPolling().subscribeBy(
                onComplete = {},
                onError = {}
            )
            EmailVeriffIntent.StartEmailVerification -> interactor.fetchEmail().flatMapObservable {
                    if (!it.verified) {
                        interactor.pollForEmailStatus().toObservable().startWith(it)
                    } else Observable.just(it)
                }.subscribeBy(onNext = {
                process(EmailVeriffIntent.EmailUpdated(it))
            }, onError = {})

            EmailVeriffIntent.ResendEmail -> interactor.fetchEmail().flatMap { interactor.resendEmail(it.address) }
                .subscribeBy(onSuccess = {
                    process(EmailVeriffIntent.EmailUpdated(it))
                }, onError = {})

            EmailVeriffIntent.UpdateEmail -> {
                check(previousState.emailInput != null)
                interactor.updateEmail(previousState.emailInput).subscribeBy(onSuccess = {
                    process(EmailVeriffIntent.EmailUpdated(it))
                }, onError = {
                    process(EmailVeriffIntent.EmailUpdateFailed)
                })
            }
            else -> null
        }
}