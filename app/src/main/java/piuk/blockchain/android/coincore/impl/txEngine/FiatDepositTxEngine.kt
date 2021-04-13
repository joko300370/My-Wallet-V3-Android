package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.models.data.BankPartner.Companion.YAPILY_DEEPLINK_PAYMENT_APPROVAL_URL
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.BankAccount
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FeeSelection
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.NeedsApprovalException
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.fiat.LinkedBankAccount
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.android.networking.PollService
import piuk.blockchain.android.ui.linkbank.BankPaymentApproval
import java.security.InvalidParameterException

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
                feeForFullAvailable = zeroFiat,
                maxLimit = limits.max,
                minLimit = limits.min,
                feeAmount = zeroFiat,
                selectedFiat = userFiat,
                feeSelection = FeeSelection()
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
                        fee = pendingTx.feeAmount
                    ),
                    TxConfirmationValue.Total(
                        total = pendingTx.amount
                    )
                )
            )
        )
    }

    override fun doUpdateFeeLevel(pendingTx: PendingTx, level: FeeLevel, customFeeAmount: Long): Single<PendingTx> {
        require(pendingTx.feeSelection.availableLevels.contains(level))
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
            walletManager.startBankTransfer(
                it.address, pendingTx.amount, pendingTx.amount.currencyCode, if (pendingTx.isOpenBankingCurrency()) {
                    YAPILY_DEEPLINK_PAYMENT_APPROVAL_URL
                } else {
                    null
                }
            )
        }.map {
            TxResult.HashedTxResult(it, pendingTx.amount)
        }

    override fun doPostExecute(pendingTx: PendingTx, txResult: TxResult): Completable =
        if (pendingTx.isOpenBankingCurrency()) {
            val paymentId = (txResult as TxResult.HashedTxResult).txId
            PollService(walletManager.getBankTransferCharge(paymentId)) {
                it.authorisationUrl != null
            }.start().map { it.value }.flatMap { bankTransferDetails ->
                walletManager.getLinkedBank(bankTransferDetails.id).map { linkedBank ->
                    bankTransferDetails.authorisationUrl?.let {
                        BankPaymentApproval(
                            paymentId,
                            it,
                            linkedBank,
                            bankTransferDetails.amount
                        )
                    } ?: throw InvalidParameterException("No auth url was returned")
                }
            }.flatMapCompletable {
                Completable.error(NeedsApprovalException(it))
            }
        } else {
            Completable.complete()
        }

    private fun PendingTx.isOpenBankingCurrency() =
        this.selectedFiat == "EUR" || this.selectedFiat == "GBP"
}