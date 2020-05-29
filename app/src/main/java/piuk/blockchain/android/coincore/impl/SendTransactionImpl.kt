package piuk.blockchain.android.coincore.impl

import info.blockchain.balance.CryptoValue
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.SendTransaction

class SendError(msg: String) : Exception(msg)

abstract class OnChainSendTransactionBase(
    override val sendingAccount: CryptoSingleAccount,
    override val address: CryptoAddress,
    protected val availableBalance: CryptoValue,
    protected val requireSecondPassword: Boolean
) : SendTransaction {

    final override fun execute(secondPassword: String): Single<String> =
        if(requireSecondPassword && secondPassword.isEmpty()) {
            Single.error(SendError("Second password not supplied"))
        } else {
            executeTransaction(secondPassword)
        }

    protected abstract fun executeTransaction(secondPassword: String): Single<String>
}
