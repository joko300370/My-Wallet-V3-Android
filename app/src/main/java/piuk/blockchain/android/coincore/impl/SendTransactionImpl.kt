package piuk.blockchain.android.coincore.impl

import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.SendTransaction
import java.lang.IllegalArgumentException

class SendError(msg: String) : Exception(msg)

abstract class OnChainSendTransactionBase(
    final override val sendingAccount: CryptoSingleAccount,
    final override val address: CryptoAddress,
    protected val requireSecondPassword: Boolean
) : SendTransaction {

    protected abstract val asset: CryptoCurrency

    init {
        require(address.address.isNotEmpty())
        require(sendingAccount.asset == address.asset)
    }

    final override var feeLevel: FeeLevel = FeeLevel.Regular
        set(value) {
            if (value in feeOptions) {
                field = value
            } else {
                throw IllegalArgumentException("Invalid Fee Level")
            }
        }

    final override fun execute(secondPassword: String): Single<String> =
        if (requireSecondPassword && secondPassword.isEmpty()) {
            Single.error(SendError("Second password not supplied"))
        } else {
            executeTransaction(secondPassword)
        }

    protected abstract fun executeTransaction(secondPassword: String): Single<String>
}
