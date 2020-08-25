package piuk.blockchain.android.ui.transfer.send

import com.blockchain.preferences.CurrencyPrefs
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
import java.lang.IllegalStateException

class SendInteractor(
    private val coincore: Coincore,
    private val currencyPrefs: CurrencyPrefs,
    private val addressFactory: AddressFactory
) {
    private lateinit var transactionProcessor: TransactionProcessor

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
                if (::transactionProcessor.isInitialized)
                    throw IllegalStateException("TxProcessor double init")
            }
            .doOnSuccess { transactionProcessor = it }
            .doOnError {
                Timber.e("!SEND!> error initialising $it")
            }.flatMapObservable {
                it.initialiseTx()
            }

    val canTransactFiat: Boolean
        get() = transactionProcessor.canTransactFiat

    fun updateTransactionAmount(amount: Money): Completable =
        transactionProcessor.updateAmount(amount)

    fun verifyAndExecute(): Completable =
        transactionProcessor.execute()

    fun modifyOptionValue(newOption: TxOptionValue): Completable =
        transactionProcessor.setOption(newOption)

    fun startFiatRateFetch(): Observable<ExchangeRate.CryptoToFiat> =
        transactionProcessor.userExchangeRate(currencyPrefs.selectedFiatCurrency)
            .map { it as ExchangeRate.CryptoToFiat }

    fun startTargetRateFetch(): Observable<ExchangeRate> =
        transactionProcessor.targetExchangeRate()

    fun validateTransaction(): Completable =
        transactionProcessor.validateAll()
}

private val Throwable.isUnexpectedContractError
    get() = (this is AddressParseError && this.error == AddressParseError.Error.ETH_UNEXPECTED_CONTRACT_ADDRESS)
