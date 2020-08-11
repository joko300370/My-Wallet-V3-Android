package piuk.blockchain.android.coincore

import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

open class TransferError(msg: String) : Exception(msg)

class TransactionValidationError(val errorCode: Int) : TransferError("Invalid Send Tx: code $errorCode") {
    companion object {
        const val HAS_TX_IN_FLIGHT = 1000
        const val INVALID_AMOUNT = 1001
        const val INSUFFICIENT_FUNDS = 1002
        const val INSUFFICIENT_GAS = 1003
        const val INVALID_ADDRESS = 1004
        const val ADDRESS_IS_CONTRACT = 1005
        const val UNSUPPORTED_OPTION = 1006
    }
}

enum class FeeLevel {
    None,
    Regular,
    Priority,
    Custom
}

data class PendingTx(
    val amount: CryptoValue,
    val available: CryptoValue,
    val fees: CryptoValue,
    val feeLevel: FeeLevel = FeeLevel.Regular,
    val options: Set<TxOptionValue> = emptySet()
) {
    fun hasOption(option: TxOption): Boolean =
        options.find { it.option == option } != null

    inline fun <reified T : TxOptionValue> getOption(option: TxOption): T? =
        options.find { it.option == option } as? T
}

enum class TxOption {
    DESCRIPTION,
    AGREEMENT,
}

sealed class TxOptionValue {
    abstract val option: TxOption

    data class TxTextOption(
        override val option: TxOption,
        val text: String = ""
    ) : TxOptionValue()
}

interface TransactionProcessor {
    val sendingAccount: CryptoAccount
    val sendTarget: ReceiveAddress

    // This may be moved into options at some point in the near future.
    val feeOptions: Set<FeeLevel>

    // Call this first to initialise the processor
    fun createPendingTx(): Single<PendingTx>

    // Set the option to the passed option value. If the option is not supported, it will not be
    // in the original list when the pendincTx is created. And if it is not supported, then trying to
    // update it will cause an error.
    fun setOption(newOption: TxOptionValue): Single<PendingTx>

    // Update the transaction with a new amount. This method should check balances, calculate fees and
    // Return a new PendingTx with the state updated fir the UI to update
    fun updateAmount(amount: CryptoValue): Single<PendingTx>

    // Check the tx is complete, well formed and possible. Complete if it is, throw an error if
    // it is not. Since the UI and Address objects should validate where possible, an error should
    // be the exception, rather than the expected case.
    fun validate(): Completable

    // Execute the transaction.
    // Ideally, I'd like to return the Tx id/hash. But we get nothing back from the
    // custodial APIs (and are not likely to, since the tx is batched and not executed immediately)
    fun execute(secondPassword: String = ""): Completable

    // Return a stream of the exchange rate between the source asset and the user's selected
    // fiat currency. This should always return at least once, but can safely either complete
    // or keep sending updated rates, depending on what is useful for Transaction context
    fun userExchangeRate(userFiat: String): Observable<ExchangeRate>

    // If the source and target assets are not the same this MAY return a stream of the exchange rates
    // between them. Or it may simply complete. This is not used yet in the UI, but it may be when
    // sell and or swap are fully integrated into this flow
    fun targetExchangeRate(): Observable<ExchangeRate>
}
