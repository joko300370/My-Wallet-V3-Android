package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
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

class FiatWithdrawalTxEngine(
    private val walletManager: CustodialWalletManager
) : TxEngine() {

    override fun assertInputsValid() {
        check(sourceAccount is FiatAccount)
        check(txTarget is LinkedBankAccount)
    }

    override val userFiat: String
        get() = (sourceAccount as FiatAccount).fiatCurrency

    override val canTransactFiat: Boolean
        get() = true

    override fun doInitialiseTx(): Single<PendingTx> {
        check(txTarget is LinkedBankAccount)
        check(sourceAccount is FiatAccount)

        return Singles.zip(
            sourceAccount.actionableBalance,
            sourceAccount.accountBalance,
            walletManager.getBankTransferLimits(userFiat, true),
            { actionableBalance, accountBalance, limits ->
                val zeroFiat = FiatValue.zero((sourceAccount as FiatAccount).fiatCurrency)
                PendingTx(
                    amount = FiatValue.zero((sourceAccount as FiatAccount).fiatCurrency),
                    maxLimit = actionableBalance,
                    minLimit = limits.min, // TODO update endpoint to get min limit from `withdrawal/fees`
                    availableBalance = actionableBalance,
                    totalBalance = accountBalance,
                    fees = zeroFiat, // TODO update endpoint to get fee from `withdrawal/fees`
                    selectedFiat = userFiat,
                    feeLevel = FeeLevel.None,
                    availableFeeLevels = setOf(FeeLevel.None)
                )
            }
        )
    }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =

        (txTarget as LinkedBankAccount).receiveAddress.flatMapCompletable {
            walletManager.createWithdrawOrder(
                amount = pendingTx.amount,
                bankId = it.address
            )
        }
            .toSingle { TxResult.UnHashedTxResult(amount = pendingTx.amount) }

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> {
        return Single.just(
            pendingTx.copy(
                confirmations = listOf(
                    TxConfirmationValue.From(sourceAccount.label),
                    TxConfirmationValue.To(txTarget.label),
                    TxConfirmationValue.FiatTxFee(
                        fee = pendingTx.fees
                    ),
                    TxConfirmationValue.EstimatedWithdrawalCompletion,
                    TxConfirmationValue.Total(
                        total = pendingTx.amount
                    )
                )
            )
        )
    }

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            pendingTx.copy(
                amount = amount
            )
        )

    override fun doUpdateFeeLevel(pendingTx: PendingTx, level: FeeLevel, customFeeAmount: Long): Single<PendingTx> {
        require(pendingTx.availableFeeLevels.contains(level))
        return Single.just(pendingTx)
    }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        doValidateAmount(pendingTx).updateTxValidity(pendingTx)

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
                    pendingTx.amount < pendingTx.minLimit -> throw TxValidationFailure(
                        ValidationState.UNDER_MIN_LIMIT
                    )
                    pendingTx.amount > pendingTx.maxLimit -> throw TxValidationFailure(
                        ValidationState.OVER_MAX_LIMIT
                    )
                    pendingTx.availableBalance < pendingTx.amount -> throw TxValidationFailure(
                        ValidationState.INSUFFICIENT_FUNDS
                    )
                    else -> Completable.complete()
                }
            } else {
                throw TxValidationFailure(ValidationState.UNKNOWN_ERROR)
            }
        }
}