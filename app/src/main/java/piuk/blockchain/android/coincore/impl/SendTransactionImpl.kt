package piuk.blockchain.android.coincore.impl

import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.PendingSendTx
import piuk.blockchain.android.coincore.SendProcessor

class SendError(msg: String) : Exception(msg)

abstract class OnChainSendTransactionBase(
    final override val sendingAccount: CryptoSingleAccount,
    final override val address: CryptoAddress,
    protected val requireSecondPassword: Boolean
) : SendProcessor {

    protected abstract val asset: CryptoCurrency

    init {
        require(address.address.isNotEmpty())
        require(sendingAccount.asset == address.asset)
    }

    final override fun execute(pendingTx: PendingSendTx, secondPassword: String): Single<String> =
        if (requireSecondPassword && secondPassword.isEmpty()) {
            Single.error(SendError("Second password not supplied"))
        } else {
            executeTransaction(pendingTx, secondPassword)
        }

    protected abstract fun executeTransaction(
        pendingTx: PendingSendTx,
        secondPassword: String
    ): Single<String>
}
