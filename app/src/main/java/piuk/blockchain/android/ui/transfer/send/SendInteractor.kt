package piuk.blockchain.android.ui.transfer.send

import io.reactivex.Single
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SendProcessor
import timber.log.Timber

class SendInteractor(
    private val coincore: Coincore
) {
    init {
        Timber.d("Constructing interactor")
    }

    private lateinit var sendProcessor: SendProcessor

    fun validatePassword(password: String): Single<Boolean> =
        Single.just(coincore.validateSecondPassword(password))



    fun fetchSendTransaction(
        sourceAccount: CryptoSingleAccount,
        targetAddress: ReceiveAddress
    ): Single<SendProcessor> =
        sourceAccount.createSendProcessor(targetAddress)
}
