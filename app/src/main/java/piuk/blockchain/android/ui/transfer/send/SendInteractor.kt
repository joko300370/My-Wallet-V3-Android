package piuk.blockchain.android.ui.transfer.send

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import piuk.blockchain.android.coincore.AddressFactory
import piuk.blockchain.android.coincore.AddressParseError
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SendTarget
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.coincore.TransactionProcessor
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import timber.log.Timber

class SendInteractor(
    private val coincore: Coincore,
    private val addressFactory: AddressFactory
) {
    private var transactionProcessor: TransactionProcessor? = null

    fun invalidateTransaction() =
        Completable.fromAction {
            transactionProcessor = null
        }

    fun validatePassword(password: String): Single<Boolean> =
        Single.just(coincore.validateSecondPassword(password))

    fun validateTargetAddress(address: String, asset: CryptoCurrency): Single<ReceiveAddress> =
        addressFactory.parse(address, asset)
            .switchIfEmpty(
                Single.error<ReceiveAddress>(
                    TxValidationFailure(ValidationState.INVALID_ADDRESS))
            )
            .onErrorResumeNext { e ->
                if (e.isUnexpectedContractError) {
                    Single.error(TxValidationFailure(ValidationState.ADDRESS_IS_CONTRACT))
                } else {
                    Single.error(e)
                }
            }

    fun initialiseTransaction(
        sourceAccount: SingleAccount,
        targetAddress: SendTarget
    ): Observable<PendingTx> =
        sourceAccount.createSendProcessor(targetAddress)
            .doOnSubscribe { Timber.e("!SEND!> SUBSCRIBE") }
            .doOnSuccess {
                if (transactionProcessor != null)
                    throw IllegalStateException("TxProcessor double init")
            }
            .doOnSuccess { transactionProcessor = it }
            .doOnError {
                Timber.e("!SEND!> error initialising $it")
            }.flatMapObservable {
                it.initialiseTx()
            }

    val canTransactFiat: Boolean
        get() = transactionProcessor?.canTransactFiat ?: throw IllegalStateException("TxProcessor not initialised")

    fun updateTransactionAmount(amount: Money): Completable =
        transactionProcessor?.updateAmount(amount) ?: throw IllegalStateException("TxProcessor not initialised")

    fun verifyAndExecute(): Completable =
        transactionProcessor?.execute() ?: throw IllegalStateException("TxProcessor not initialised")

    fun modifyOptionValue(newOption: TxOptionValue): Completable =
        transactionProcessor?.setOption(newOption) ?: throw IllegalStateException("TxProcessor not initialised")

    fun startFiatRateFetch(): Observable<ExchangeRate.CryptoToFiat> =
        transactionProcessor?.userExchangeRate()
            ?.map { it as ExchangeRate.CryptoToFiat }
            ?: throw IllegalStateException("TxProcessor not initialised")

    fun startTargetRateFetch(): Observable<ExchangeRate> =
        transactionProcessor?.targetExchangeRate() ?: throw IllegalStateException("TxProcessor not initialised")

    fun validateTransaction(): Completable =
        transactionProcessor?.validateAll() ?: throw IllegalStateException("TxProcessor not initialised")
}

private val Throwable.isUnexpectedContractError
    get() = (this is AddressParseError && this.error == AddressParseError.Error.ETH_UNEXPECTED_CONTRACT_ADDRESS)
