package piuk.blockchain.android.coincore

import info.blockchain.balance.CryptoValue
import io.reactivex.Completable
import io.reactivex.Single
import java.lang.Exception

class SendValidationError(val errorCode: Int) : Exception("Invalid Send Tx: code $errorCode") {

    companion object {
        const val HAS_TX_IN_FLIGHT = 1000
        const val INVALID_AMOUNT = 1001
        const val INSUFFICIENT_FUNDS = 1002
    }
}

enum class FeeLevel {
    None,
    Regular,
    Priority,
    Custom
}

data class PendingSendTx(
    val amount: CryptoValue,
    val feeLevel: FeeLevel = FeeLevel.Regular,
    val notes: String = ""
)

interface SendProcessor {
    val sendingAccount: CryptoSingleAccount
    val address: ReceiveAddress

    val feeOptions: Set<FeeLevel>

    fun availableBalance(pendingTx: PendingSendTx): Single<CryptoValue>
    fun absoluteFee(pendingTx: PendingSendTx): Single<CryptoValue>

    // Check the tx is complete, well formed and possible. Complete if it is, throw an error if
    // it is not. Since the UI and Address objects should validate where possible, an error should
    // be the exception, rather than the expected case.
    fun validate(pendingTx: PendingSendTx): Completable

    // Execute the transaction.
    // Ideally, I'd like to return the Tx id/hash. But we get nothing back from the
    // custodial APIs (and are not likely to, since the tx is batched and not executed immediately)
    fun execute(pendingTx: PendingSendTx, secondPassword: String = ""): Completable
}
