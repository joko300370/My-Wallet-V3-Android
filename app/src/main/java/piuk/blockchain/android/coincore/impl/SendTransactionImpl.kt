package piuk.blockchain.android.coincore.impl

import info.blockchain.balance.CryptoCurrency
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.PendingSendTx
import piuk.blockchain.android.coincore.SendProcessor

class SendError(msg: String) : Exception(msg)

abstract class OnChainSendProcessorBase(
    final override val sendingAccount: CryptoAccount,
    final override val address: CryptoAddress,
    private val requireSecondPassword: Boolean
) : SendProcessor {

    protected abstract val asset: CryptoCurrency

    init {
        require(address.address.isNotEmpty())
        require(sendingAccount.asset == address.asset)
    }

    final override fun execute(pendingTx: PendingSendTx, secondPassword: String): Completable =
        if (requireSecondPassword && secondPassword.isEmpty()) {
            Completable.error(SendError("Second password not supplied"))
        } else {
            executeTransaction(pendingTx, secondPassword)
                .ignoreElement()
        }

    protected abstract fun executeTransaction(
        pendingTx: PendingSendTx,
        secondPassword: String
    ): Single<String>
}
