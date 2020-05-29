package piuk.blockchain.android.coincore

import info.blockchain.balance.CryptoValue
import io.reactivex.Single

enum class ValidationState {
    VALID,
    // Errors
}

sealed class FeeSchedule {
    abstract val label: String

    object None : FeeSchedule() {
        override val label: String = "None"
    }

//    class PriorityFee : FeeSchedule
//    class StandardFee : FeeSchedule
//    class CustomFee : FeeSchedule
}

interface SendTransaction {
    val sendingAccount: CryptoSingleAccount
    val address: ReceiveAddress
    var amount: CryptoValue
    var fees: FeeSchedule
    var notes: String

    fun canExecute(): Single<ValidationState>
    fun absoluteFee(): Single<CryptoValue>
    fun execute(secondPassword: String = ""): Single<String>
}
