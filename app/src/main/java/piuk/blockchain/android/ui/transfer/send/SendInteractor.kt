package piuk.blockchain.android.ui.transfer.send

import info.blockchain.balance.CryptoValue
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.PendingSendTx
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SendProcessor
import piuk.blockchain.androidcore.utils.extensions.thenSingle
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

    fun initialiseTransaction(
        sourceAccount: CryptoSingleAccount,
        targetAddress: ReceiveAddress
    ): Completable =
        sourceAccount.createSendProcessor(targetAddress)
            .doOnSuccess { sendProcessor= it }
            .ignoreElement()

    fun getAvailableBalance(tx: PendingSendTx): Single<CryptoValue> =
        sendProcessor.availableBalance(tx)

    fun verifyAndExecute(tx:PendingSendTx): Single<String> =
        sendProcessor.validate(tx)
            .thenSingle {
                sendProcessor.execute(tx)
            }
}
