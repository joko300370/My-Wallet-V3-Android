package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.updateTxValidity

// Transfer from a custodial trading account to an onChain non-custodial account
class TradingToOnChainTxEngine(
    private val isNoteSupported: Boolean,
    private val walletManager: CustodialWalletManager
) : TxEngine() {

    override fun assertInputsValid() {
        require(txTarget is CryptoAddress)
        require(sourceAccount.asset == (txTarget as CryptoAddress).asset)
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        Single.just(
            PendingTx(
                amount = CryptoValue.zero(sourceAccount.asset),
                available = CryptoValue.zero(sourceAccount.asset),
                fees = CryptoValue.zero(sourceAccount.asset),
                feeLevel = FeeLevel.None,
                selectedFiat = userFiat
            )
        )

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        require(amount is CryptoValue)
        require(amount.currency == asset)

        return sourceAccount.actionableBalance
            .map { it as CryptoValue }
            .map { available ->
                pendingTx.copy(
                    amount = amount,
                    available = available
                )
            }
    }

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            pendingTx.copy(options = listOf(
                TxOptionValue.From(from = sourceAccount.label),
                TxOptionValue.To(to = txTarget.label),
                TxOptionValue.FeedTotal(amount = pendingTx.amount, fee = pendingTx.fees)
            ).apply {
                if (isNoteSupported) {
                    toMutableList().add(TxOptionValue.Description())
                }
            }))

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx).updateTxValidity(pendingTx)

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx).updateTxValidity(pendingTx)

    private fun validateAmounts(pendingTx: PendingTx): Completable =
        sourceAccount.actionableBalance
            .flatMapCompletable { max ->
                if (max >= pendingTx.amount) {
                    Completable.complete()
                } else {
                    throw TxValidationFailure(ValidationState.INVALID_AMOUNT)
                }
            }

    // The custodial balance now returns an id, so it is possible to add a note via this
    // processor at some point. TODO
    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> {
        val targetAddress = txTarget as CryptoAddress
        return walletManager.transferFundsToWallet(pendingTx.amount as CryptoValue, targetAddress.address)
            .map {
                TxResult.UnHashedTxResult(pendingTx.amount)
            }
    }
}
