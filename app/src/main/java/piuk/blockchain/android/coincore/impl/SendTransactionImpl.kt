package piuk.blockchain.android.coincore.impl

import info.blockchain.balance.CryptoCurrency
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.SendValidationError
import piuk.blockchain.android.coincore.TransactionProcessor
import piuk.blockchain.android.coincore.TxOptionValue

class SendError(msg: String) : Exception(msg)

abstract class TransactionProcessorBase : TransactionProcessor {

    protected abstract var pendingTx: PendingTx

    override fun createPendingTx(): Single<PendingTx> =
        Single.just(pendingTx)

    final override fun setOption(newOption: TxOptionValue): Single<PendingTx> {
        val pendingTx = this.pendingTx
        return if (pendingTx.hasOption(newOption.option)) {
            val opts = pendingTx.options.toMutableSet()
            val old = opts.find { it.option == newOption.option }
            opts.remove(old)
            opts.add(newOption)
            this.pendingTx = pendingTx.copy(options = opts)
            Single.just(this.pendingTx)
        } else {
            Single.error(SendValidationError(SendValidationError.UNSUPPORTED_OPTION))
        }
    }
}

abstract class OnChainSendProcessorBase(
    final override val sendingAccount: CryptoAccount,
    final override val sendTarget: CryptoAddress,
    private val requireSecondPassword: Boolean
) : TransactionProcessorBase() {

    protected abstract val asset: CryptoCurrency

    init {
        require(sendTarget.address.isNotEmpty())
        require(sendingAccount.asset == sendTarget.asset)
    }

    final override fun execute(pendingTx: PendingTx, secondPassword: String): Completable =
        if (requireSecondPassword && secondPassword.isEmpty()) {
            Completable.error(SendError("Second password not supplied"))
        } else {
            executeTransaction(pendingTx, secondPassword)
                .ignoreElement()
        }

    protected abstract fun executeTransaction(
        pendingTx: PendingTx,
        secondPassword: String
    ): Single<String>
}
