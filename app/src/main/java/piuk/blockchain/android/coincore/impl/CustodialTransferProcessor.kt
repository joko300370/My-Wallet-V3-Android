package piuk.blockchain.android.coincore.impl

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoValue
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.SendValidationError
import piuk.blockchain.android.coincore.TxOption
import piuk.blockchain.android.coincore.TxOptionValue

class CustodialTransferProcessor(
    private val isNoteSupported: Boolean,
    override val sendingAccount: CryptoAccount,
    override val sendTarget: CryptoAddress,
    private val walletManager: CustodialWalletManager
) : TransactionProcessorBase() {

    init {
        require(sendingAccount.asset == sendTarget.asset)
    }

    override val feeOptions = setOf(FeeLevel.None)

    override var pendingTx: PendingTx =
        PendingTx(
            amount = CryptoValue.zero(sendingAccount.asset),
            feeLevel = FeeLevel.Regular,
            options = if (isNoteSupported) {
                setOf(TxOptionValue.TxTextOption(TxOption.DESCRIPTION))
            } else {
                emptySet()
            }
        )

    override fun availableBalance(pendingTx: PendingTx): Single<CryptoValue> =
        sendingAccount.balance
            .map { it as CryptoValue }

    override fun absoluteFee(pendingTx: PendingTx): Single<CryptoValue> =
        Single.just(CryptoValue.zero(sendingAccount.asset))

    override fun validate(pendingTx: PendingTx): Completable =
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

    override fun execute(pendingTx: PendingTx, secondPassword: String): Completable =
        walletManager.transferFundsToWallet(pendingTx.amount as CryptoValue, sendTarget.address)
}
