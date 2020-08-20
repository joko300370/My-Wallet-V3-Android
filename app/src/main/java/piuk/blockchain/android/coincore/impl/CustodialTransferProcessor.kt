package piuk.blockchain.android.coincore.impl

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionProcessor
import piuk.blockchain.android.coincore.TxOption
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

// Transfer from a custodial trading account to an onChain non-custodial account
open class CustodialTransferProcessor(
    private val isNoteSupported: Boolean,
    sendingAccount: CryptoAccount,
    sendTarget: CryptoAddress,
    private val walletManager: CustodialWalletManager,
    exchangeRates: ExchangeRateDataManager
) : TransactionProcessor(
    sendingAccount,
    sendTarget,
    exchangeRates
) {
    init {
        require(sendingAccount.asset == sendTarget.asset)
    }

    override val feeOptions = setOf(FeeLevel.None)

    override fun doInitialiseTx(): Single<PendingTx> =
        Single.just(
            PendingTx(
                amount = CryptoValue.zero(sendingAccount.asset),
                available = CryptoValue.zero(sendingAccount.asset),
                fees = CryptoValue.zero(sendingAccount.asset),
                feeLevel = FeeLevel.None,
                options = if (isNoteSupported) {
                    setOf(TxOptionValue.TxTextOption(TxOption.DESCRIPTION))
                } else {
                    emptySet()
                }
            )
        )

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        require(amount is CryptoValue)
        require(amount.currency == asset)

        return sendingAccount.balance
            .map { it as CryptoValue }
            .map { available ->
                pendingTx.copy(
                    amount = amount,
                    available = available
                )
            }
    }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx).updateTxValidity(pendingTx)

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx).updateTxValidity(pendingTx)

    private fun validateAmounts(pendingTx: PendingTx): Completable =
        sendingAccount.balance
            .flatMapCompletable { max ->
                if (max >= pendingTx.amount) {
                    Completable.complete()
                } else {
                    throw TxValidationFailure(ValidationState.INVALID_AMOUNT)
                }
            }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Completable {
        require(sendTarget is CryptoAddress)
        return walletManager.transferFundsToWallet(pendingTx.amount as CryptoValue, sendTarget.address)
    }
}
