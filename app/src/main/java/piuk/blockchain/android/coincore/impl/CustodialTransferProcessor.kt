package piuk.blockchain.android.coincore.impl

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoValue
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingSendTx
import piuk.blockchain.android.coincore.SendProcessor
import piuk.blockchain.android.coincore.SendValidationError

class CustodialTransferProcessor(
    override val isNoteSupported: Boolean,
    override val sendingAccount: CryptoAccount,
    override val sendTarget: CryptoAddress,
    private val walletManager: CustodialWalletManager
) : SendProcessor {

    init {
        require(sendingAccount.asset == sendTarget.asset)
    }

    override val feeOptions = setOf(FeeLevel.None)

    override fun availableBalance(pendingTx: PendingSendTx): Single<CryptoValue> =
        sendingAccount.balance
            .map { it as CryptoValue }

    override fun absoluteFee(pendingTx: PendingSendTx): Single<CryptoValue> =
        Single.just(CryptoValue.zero(sendingAccount.asset))

    override fun validate(pendingTx: PendingSendTx): Completable =
        availableBalance(pendingTx)
            .flatMapCompletable { max ->
                if (max >= pendingTx.amount) {
                    Completable.complete()
                } else {
                    Completable.error(
                        SendValidationError(SendValidationError.INSUFFICIENT_FUNDS)
                    )
                }
            }

    override fun execute(pendingTx: PendingSendTx, secondPassword: String): Completable =
        walletManager.transferFundsToWallet(pendingTx.amount as CryptoValue, sendTarget.address)
}
