package piuk.blockchain.android.coincore.impl

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoValue
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionValidationError
import piuk.blockchain.android.coincore.TxOption
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

open class CustodialTransferProcessor(
    isNoteSupported: Boolean,
    final override val sendingAccount: CryptoAccount,
    final override val sendTarget: CryptoAddress,
    private val walletManager: CustodialWalletManager,
    exchangeRates: ExchangeRateDataManager
) : TransactionProcessorBase(exchangeRates) {

    init {
        require(sendingAccount.asset == sendTarget.asset)
    }

    override val feeOptions = setOf(FeeLevel.None)

    override var pendingTx: PendingTx =
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

    override fun updateAmount(amount: CryptoValue): Single<PendingTx> =
        sendingAccount.balance
            .map { it as CryptoValue }
            .map { available ->
                if (amount <= available) {
                    pendingTx.copy(
                        amount = amount,
                        available = available
                    )
                } else {
                    throw TransactionValidationError(TransactionValidationError.INSUFFICIENT_FUNDS)
                }
            }
            .doOnSuccess { this.pendingTx = it }

    override fun validate(): Completable =
        sendingAccount.balance
            .flatMapCompletable { max ->
                if (max >= pendingTx.amount) {
                    Completable.complete()
                } else {
                    Completable.error(
                        TransactionValidationError(TransactionValidationError.INSUFFICIENT_FUNDS)
                    )
                }
            }

    override fun execute(secondPassword: String): Completable =
        walletManager.transferFundsToWallet(pendingTx.amount as CryptoValue, sendTarget.address)
}
