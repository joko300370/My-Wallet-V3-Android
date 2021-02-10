package piuk.blockchain.android.ui.kyc.email.entry

import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.networking.PollService
import piuk.blockchain.androidcore.data.settings.Email
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater

class EmailVerifyInteractor(private val emailUpdater: EmailSyncUpdater) {

    private val pollEmail = PollService(
        emailUpdater.email()
    ) {
        it.verified
    }

    fun fetchEmail(): Single<Email> =
        emailUpdater.email()

    fun pollForEmailStatus(): Single<Email> {

        return pollEmail.start(timerInSec = 1, retries = Integer.MAX_VALUE).map {
            it.value
        }
    }

    fun updateEmail(email: String): Single<Email> =
        emailUpdater.updateEmailAndSync(email)

    fun cancelPolling(): Completable =
        Completable.fromCallable {
            pollEmail.cancel.onNext(true)
        }
}