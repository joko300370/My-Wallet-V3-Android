package piuk.blockchain.android.coincore

import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Single

open class TransferError(msg: String) : Exception(msg)

class SendValidationError(val errorCode: Int) : TransferError("Invalid Send Tx: code $errorCode") {
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
    val amount: Money,
    val feeLevel: FeeLevel = FeeLevel.Regular,
    val options: Set<TxOptionValue> = emptySet()
) {
    @Suppress("UNCHECKED_CAST")
    fun <T : TxOptionValue> getOption(option:TxOption): T? =
        options.find { it.option == option } as? T

    fun hasOption(option: TxOption): Boolean =
        options.find { it.option == option } != null
}

enum class TxOption {
    DESCRIPTION,
    AGREEMENT,
}

sealed class TxOptionValue {
    abstract val option: TxOption
//
//    override fun equals(other: Any?): Boolean {
//
//    }

    data class TxTextOption(
        override val option: TxOption,
        val text: String = ""
    ) : TxOptionValue()
}

interface TransactionProcessor {
    val sendingAccount: CryptoAccount
    val sendTarget: ReceiveAddress

    val feeOptions: Set<FeeLevel>

    fun createPendingTx(): Single<PendingTx>

    fun setOption(newOption: TxOptionValue): Single<PendingTx>

    fun availableBalance(pendingTx: PendingTx): Single<CryptoValue>

    fun absoluteFee(pendingTx: PendingTx): Single<CryptoValue>

    // Check the tx is complete, well formed and possible. Complete if it is, throw an error if
    // it is not. Since the UI and Address objects should validate where possible, an error should
    // be the exception, rather than the expected case.
    fun validate(pendingTx: PendingTx): Completable

    // Execute the transaction.
    // Ideally, I'd like to return the Tx id/hash. But we get nothing back from the
    // custodial APIs (and are not likely to, since the tx is batched and not executed immediately)
    fun execute(pendingTx: PendingTx, secondPassword: String = ""): Completable

//    fun userExchangeRate(userFiat: String): Observable<ExchangeRate>
//    fun targetExchangeRate(): Observable<ExchangeRate>
}
