package piuk.blockchain.android.coincore

import info.blockchain.balance.CryptoValue
import io.reactivex.Completable
import io.reactivex.Single
import java.lang.Exception

class SendValidationError(val errorCode: Int) : Exception("Invalid Send Tx") {

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

interface SendTransaction {
    val sendingAccount: CryptoSingleAccount
    val address: ReceiveAddress
    var amount: CryptoValue
    var feeLevel: FeeLevel
    var notes: String

    val feeOptions: Set<FeeLevel>

    val availableBalance: Single<CryptoValue>
    val absoluteFee: Single<CryptoValue>

    // Check the tx is complete, well formed and possible. Complete if it is, throw an error if
    // it is not. Since the UI and Address objects should validate where possible, an error should
    // be the exception, rather than the expected case.
    fun validate(): Completable
    // Execute the transaction and return the transaction id - either the hash or a custodial Id
    fun execute(secondPassword: String = ""): Single<String>
}
