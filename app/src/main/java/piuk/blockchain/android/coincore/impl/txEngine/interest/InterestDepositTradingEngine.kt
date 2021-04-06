package piuk.blockchain.android.coincore.impl.txEngine.interest

import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FeeSelection
import piuk.blockchain.android.coincore.InterestAccount
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TradingAccount
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.updateTxValidity

class InterestDepositTradingEngine(private val walletManager: CustodialWalletManager) : InterestEngine(walletManager) {

    override fun assertInputsValid() {
        check(sourceAccount is TradingAccount)
        check(txTarget is InterestAccount)
    }

    private val availableBalance: Single<Money>
        get() = sourceAccount.accountBalance

    override fun doInitialiseTx(): Single<PendingTx> {
        return getLimits().zipWith(availableBalance)
            .map { (limits, balance) ->
                PendingTx(
                    amount = CryptoValue.zero(sourceAsset),
                    minLimit = limits.minDepositAmount,
                    feeSelection = FeeSelection(),
                    selectedFiat = userFiat,
                    availableBalance = balance,
                    totalBalance = balance,
                    feeAmount = CryptoValue.zero(sourceAsset),
                    feeForFullAvailable = CryptoValue.zero(sourceAsset)
                )
            }
    }

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> =
        availableBalance.map { balance ->
            balance as CryptoValue
        }.map { available ->
            pendingTx.copy(
                amount = amount,
                availableBalance = available,
                totalBalance = available
            )
        }

    override fun doOptionUpdateRequest(pendingTx: PendingTx, newConfirmation: TxConfirmationValue): Single<PendingTx> =
        if (newConfirmation.confirmation.isInterestAgreement()) {
            Single.just(pendingTx.addOrReplaceOption(newConfirmation))
        } else {
            Single.just(
                modifyEngineConfirmations(
                    pendingTx = pendingTx
                )
            )
        }

    override fun doUpdateFeeLevel(pendingTx: PendingTx, level: FeeLevel, customFeeAmount: Long): Single<PendingTx> {
        return Single.just(pendingTx)
    }

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            pendingTx.copy(
                confirmations = listOf(
                    TxConfirmationValue.From(from = sourceAccount.label),
                    TxConfirmationValue.To(to = txTarget.label),
                    TxConfirmationValue.Total(
                        total = pendingTx.amount,
                        exchange = pendingTx.amount.toFiat(exchangeRates, userFiat)
                    )
                )
            )
        ).map {
            modifyEngineConfirmations(it)
        }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        availableBalance.flatMapCompletable { balance ->
            if (pendingTx.amount <= balance) {
                checkIfAmountIsBelowMinLimit(pendingTx)
            } else {
                throw TxValidationFailure(ValidationState.INSUFFICIENT_FUNDS)
            }
        }.updateTxValidity(pendingTx)

    private fun checkIfAmountIsBelowMinLimit(pendingTx: PendingTx) =
        when {
            pendingTx.minLimit == null -> {
                throw TxValidationFailure(ValidationState.UNINITIALISED)
            }
            pendingTx.amount < pendingTx.minLimit -> throw TxValidationFailure(ValidationState.UNDER_MIN_LIMIT)
            else -> Completable.complete()
        }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> {
        val px = if (!areOptionsValid(pendingTx)) {
            pendingTx.copy(validationState = ValidationState.OPTION_INVALID)
        } else {
            pendingTx.copy(validationState = ValidationState.CAN_EXECUTE)
        }
        return Single.just(px)
    }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        walletManager.executeCustodialTransfer(
            amount = pendingTx.amount,
            origin = Product.SIMPLEBUY,
            destination = Product.SAVINGS
        ).toSingle {
            TxResult.UnHashedTxResult(pendingTx.amount)
        }
}