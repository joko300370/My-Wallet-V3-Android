package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.BankAccount
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.fiat.LinkedBankAccount
import piuk.blockchain.android.coincore.updateTxValidity

class FiatDepositTxEngine(
    private val walletManager: CustodialWalletManager
) : TxEngine() {

    override fun assertInputsValid() {
        check(sourceAccount is BankAccount)
        check(txTarget is FiatAccount)
    }

    override val userFiat: String
        get() = (txTarget as FiatAccount).fiatCurrency

    override fun doInitialiseTx(): Single<PendingTx> {
        check(sourceAccount is BankAccount)
        check(txTarget is FiatAccount)

        return walletManager.getBankTransferLimits(userFiat, true).map { limits ->
            val zeroFiat = FiatValue.zero((sourceAccount as LinkedBankAccount).fiatCurrency)
            PendingTx(
                amount = zeroFiat,
                totalBalance = zeroFiat,
                availableBalance = zeroFiat,
                maxLimit = limits.max,
                minLimit = limits.min,
                fees = zeroFiat,
                selectedFiat = userFiat,
                feeLevel = FeeLevel.None,
                availableFeeLevels = setOf(FeeLevel.None)
            )
        }
    }

    override val canTransactFiat: Boolean
        get() = true

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            pendingTx.copy(
                amount = amount
            )
        )

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> {
        return Single.just(
            pendingTx.copy(
                confirmations = listOf(
                    TxConfirmationValue.From(sourceAccount.label),
                    TxConfirmationValue.To(txTarget.label),
                    TxConfirmationValue.EstimatedDepositCompletion,
                    TxConfirmationValue.FiatTxFee(
                        fee = pendingTx.fees
                    ),
                    TxConfirmationValue.Total(
                        total = pendingTx.amount
                    )
                )
            )
        )
    }

    override fun doUpdateFeeLevel(pendingTx: PendingTx, level: FeeLevel, customFeeAmount: Long): Single<PendingTx> {
        require(pendingTx.availableFeeLevels.contains(level))
        // This engine only supports FeeLevel.None, so
        return Single.just(pendingTx)
    }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> {
        return if (pendingTx.validationState == ValidationState.UNINITIALISED && pendingTx.amount.isZero) {
            Single.just(pendingTx)
        } else {
            validateAmount(pendingTx).updateTxValidity(pendingTx)
        }
    }

    private fun validateAmount(pendingTx: PendingTx): Completable =
        Completable.fromCallable {
            if (pendingTx.maxLimit != null && pendingTx.minLimit != null) {
                when {
                    pendingTx.amount.isZero -> throw TxValidationFailure(ValidationState.INVALID_AMOUNT)
                    pendingTx.amount < pendingTx.minLimit -> throw TxValidationFailure(
                        ValidationState.UNDER_MIN_LIMIT
                    )
                    pendingTx.amount > pendingTx.maxLimit -> throw TxValidationFailure(
                        ValidationState.OVER_MAX_LIMIT
                    )
                    else -> Completable.complete()
                }
            } else {
                throw TxValidationFailure(ValidationState.UNKNOWN_ERROR)
            }
        }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        doValidateAmount(pendingTx).updateTxValidity(pendingTx)

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        sourceAccount.receiveAddress.flatMap {
            walletManager.startBankTransfer(it.address, pendingTx.amount, pendingTx.amount.currencyCode)
        }.map {
            TxResult.HashedTxResult(it, pendingTx.amount)
        }
}