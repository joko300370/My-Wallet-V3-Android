package piuk.blockchain.android.coincore.impl.txEngine.interest

import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.repositories.interest.InterestLimits
import info.blockchain.balance.Money
import io.reactivex.Single
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxConfirmation
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.TxFee

abstract class InterestEngine(private val walletManager: CustodialWalletManager) : TxEngine() {

    protected fun modifyEngineConfirmations(
        pendingTx: PendingTx,
        termsChecked: Boolean = getTermsOptionValue(pendingTx),
        agreementChecked: Boolean = getTermsOptionValue(pendingTx)
    ): PendingTx =
        pendingTx.removeOption(TxConfirmation.DESCRIPTION)
            .removeOption(TxConfirmation.FEE_SELECTION)
            .addOrReplaceOption(
                TxConfirmationValue.NetworkFee(
                    txFee = TxFee(
                        pendingTx.feeAmount,
                        TxFee.FeeType.DEPOSIT_FEE,
                        sourceAsset
                    )
                )
            )
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

    protected fun getLimits(): Single<InterestLimits> =
        walletManager.getInterestLimits(sourceAsset).toSingle()

    protected fun areOptionsValid(pendingTx: PendingTx): Boolean {
        val terms = getTermsOptionValue(pendingTx)
        val agreement = getAgreementOptionValue(pendingTx)
        return (terms && agreement)
    }

    private fun getTermsOptionValue(pendingTx: PendingTx): Boolean =
        pendingTx.getOption<TxConfirmationValue.TxBooleanConfirmation<Unit>>(
            TxConfirmation.AGREEMENT_INTEREST_T_AND_C
        )?.value ?: false

    private fun getAgreementOptionValue(pendingTx: PendingTx): Boolean =
        pendingTx.getOption<TxConfirmationValue.TxBooleanConfirmation<Money>>(
            TxConfirmation.AGREEMENT_INTEREST_TRANSFER
        )?.value ?: false

    protected fun TxConfirmation.isInterestAgreement(): Boolean = this in setOf(
        TxConfirmation.AGREEMENT_INTEREST_T_AND_C,
        TxConfirmation.AGREEMENT_INTEREST_TRANSFER
    )
}