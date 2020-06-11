package piuk.blockchain.android.ui.transfer.send

import io.reactivex.Single
import piuk.blockchain.android.coincore.Coincore

class SendInteractor(
    private val coincore: Coincore
) {
    fun validatePassword(password: String): Single<Boolean> = Single.just(
        coincore.validateSecondPassword(password))
}
