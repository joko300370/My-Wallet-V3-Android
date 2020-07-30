package piuk.blockchain.android.ui.transfer.send

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.AddressFactory
import piuk.blockchain.android.coincore.AddressParseError
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.PendingSendTx
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SendProcessor
import piuk.blockchain.android.coincore.SendTarget
import piuk.blockchain.android.coincore.SendValidationError
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.androidcore.utils.extensions.then

class SendInteractor(
    private val coincore: Coincore,
    private val addressFactory: AddressFactory
) {
    private lateinit var sendProcessor: SendProcessor

    fun validatePassword(password: String): Single<Boolean> =
        Single.just(coincore.validateSecondPassword(password))

    fun validateTargetAddress(address: String, asset: CryptoCurrency): Single<ReceiveAddress> =
        addressFactory.parse(address, asset)
            .switchIfEmpty(
                Single.error<ReceiveAddress>(SendValidationError(SendValidationError.INVALID_ADDRESS))
            )
            .onErrorResumeNext { e ->
                if (e.isUnexpectedContractError) {
                    Single.error(SendValidationError(SendValidationError.ADDRESS_IS_CONTRACT))
                } else {
                    Single.error(e)
                }
            }

    fun initialiseTransaction(
        sourceAccount: SingleAccount,
        targetAddress: SendTarget
    ): Completable =
        sourceAccount.createSendProcessor(targetAddress)
            .doOnSuccess { sendProcessor = it }
            .ignoreElement()

    fun getAvailableBalance(tx: PendingSendTx): Single<CryptoValue> =
        sendProcessor.availableBalance(tx)

    fun verifyAndExecute(tx: PendingSendTx): Completable =
        sendProcessor.validate(tx)
            .then {
                sendProcessor.execute(tx)
            }

    fun getFeeForTransaction(tx: PendingSendTx) = sendProcessor.absoluteFee(tx)

    fun checkIfNoteSupported(): Single<Boolean> = Single.just(sendProcessor.isNoteSupported)
}

private val Throwable.isUnexpectedContractError
    get() = (this is AddressParseError && this.error == AddressParseError.Error.ETH_UNEXPECTED_CONTRACT_ADDRESS)
