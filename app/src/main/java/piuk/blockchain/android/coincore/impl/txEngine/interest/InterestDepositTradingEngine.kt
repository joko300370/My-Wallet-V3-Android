package piuk.blockchain.android.coincore.impl.txEngine.interest

import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FeeSelection
import piuk.blockchain.android.coincore.InterestAccount
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TradingAccount
import piuk.blockchain.android.coincore.TxConfirmation
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.updateTxValidity

class InterestDepositTradingEngine(private val walletManager: CustodialWalletManager) : TxEngine() {

    override fun assertInputsValid() {
        check(sourceAccount is TradingAccount)
        check(txTarget is InterestAccount)
    }

    private val availableBalance: Single<Money>
        get() = sourceAccount.accountBalance

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            modifyEngineConfirmations(pendingTx)
        )

    private fun modifyEngineConfirmations(
        pendingTx: PendingTx,
        termsChecked: Boolean = false,
        agreementChecked: Boolean = false
    ): PendingTx =
        pendingTx
            .addOrReplaceOption(
                TxConfirmationValue.TxBooleanConfirmation<Unit>(
                    confirmation = TxConfirmation.AGREEMENT_INTEREST_T_AND_C,
                    value = termsChecked
                )
            )
            .addOrReplaceOption(
                TxConfirmationValue.TxBooleanConfirmation(
                    confirmation = TxConfirmation.AGREEMENT_INTEREST_TRANSFER,
                    data = pendingTx.amount,
                    value = agreementChecked
                )
            )

    override fun doOptionUpdateRequest(pendingTx: PendingTx, newConfirmation: TxConfirmationValue): Single<PendingTx> =
        if (newConfirmation.confirmation in setOf(
                TxConfirmation.AGREEMENT_INTEREST_T_AND_C,
                TxConfirmation.AGREEMENT_INTEREST_TRANSFER
            )
        ) {
            Single.just(pendingTx.addOrReplaceOption(newConfirmation))
        } else {
            Single.just(
                modifyEngineConfirmations(
                    pendingTx = pendingTx,
                    termsChecked = getTermsOptionValue(pendingTx),
                    agreementChecked = getAgreementOptionValue(pendingTx)
                )
            )
        }

    private fun getTermsOptionValue(pendingTx: PendingTx): Boolean =
        pendingTx.getOption<TxConfirmationValue.TxBooleanConfirmation<Unit>>(
            TxConfirmation.AGREEMENT_INTEREST_T_AND_C
        )?.value ?: false

    private fun getAgreementOptionValue(pendingTx: PendingTx): Boolean =
        pendingTx.getOption<TxConfirmationValue.TxBooleanConfirmation<Money>>(
            TxConfirmation.AGREEMENT_INTEREST_TRANSFER
        )?.value ?: false

    override fun doInitialiseTx(): Single<PendingTx> {
        return walletManager.getInterestLimits(sourceAsset).toSingle().zipWith(availableBalance)
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

    override fun doUpdateFeeLevel(pendingTx: PendingTx, level: FeeLevel, customFeeAmount: Long): Single<PendingTx> {
        return Single.just(pendingTx)
    }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> {
        val minLimit =
            pendingTx.minLimit ?: return Single.just(pendingTx.copy(validationState = ValidationState.UNINITIALISED))
        return Single.just(
            if (pendingTx.amount < minLimit) {
                pendingTx.copy(validationState = ValidationState.UNDER_MIN_LIMIT)
            } else {
                pendingTx
            }
        ).updateTxValidity(pendingTx)
    }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> {
        val px = if (pendingTx.validationState == ValidationState.CAN_EXECUTE && !areOptionsValid(pendingTx)) {
            pendingTx.copy(validationState = ValidationState.OPTION_INVALID)
        } else {
            pendingTx
        }
        return Single.just(px)
    }

    private fun areOptionsValid(pendingTx: PendingTx): Boolean {
        val terms = getTermsOptionValue(pendingTx)
        val agreement = getAgreementOptionValue(pendingTx)
        return (terms && agreement)
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