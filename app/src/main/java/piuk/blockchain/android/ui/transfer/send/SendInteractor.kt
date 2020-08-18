package piuk.blockchain.android.ui.transfer.send

import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
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
import piuk.blockchain.android.coincore.TransactionValidationError
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.androidcore.utils.extensions.then
import timber.log.Timber

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
                Single.error<ReceiveAddress>(TransactionValidationError(TransactionValidationError.INVALID_ADDRESS))
            )
            .onErrorResumeNext { e ->
                if (e.isUnexpectedContractError) {
                    Single.error(TransactionValidationError(TransactionValidationError.ADDRESS_IS_CONTRACT))
                } else {
                    Single.error(e)
                }
            }

    fun initialiseTransaction(
        sourceAccount: SingleAccount,
        targetAddress: SendTarget
    ): Single<PendingTx> =
        sourceAccount.createSendProcessor(targetAddress)
            .doOnSuccess { transactionProcessor = it }
            .flatMap { it.createPendingTx() }
            .doOnError {
                Timber.e("---- error initialising $it")
            }

    fun updateTransactionAmount(amount: CryptoValue): Single<PendingTx> =
        transactionProcessor.updateAmount(amount)

    fun verifyAndExecute(): Completable =
        transactionProcessor.validate()
            .then { transactionProcessor.execute() }

    fun modifyOptionValue(newOption: TxOptionValue): Single<PendingTx> =
        transactionProcessor.setOption(newOption)

    fun startFiatRateFetch(): Observable<ExchangeRate.CryptoToFiat> =
        transactionProcessor.userExchangeRate(currencyPrefs.selectedFiatCurrency)
            .map { it as ExchangeRate.CryptoToFiat }

    fun startTargetRateFetch(): Observable<ExchangeRate> =
        transactionProcessor.targetExchangeRate()
}

private val Throwable.isUnexpectedContractError
    get() = (this is AddressParseError && this.error == AddressParseError.Error.ETH_UNEXPECTED_CONTRACT_ADDRESS)
