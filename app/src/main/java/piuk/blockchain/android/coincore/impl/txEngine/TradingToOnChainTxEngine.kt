package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.featureflags.GatedFeature
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FeeSelection
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.android.ui.transactionflow.flow.FeeInfo

// Transfer from a custodial trading account to an onChain non-custodial account
class TradingToOnChainTxEngine(
    private val isNoteSupported: Boolean,
    private val walletManager: CustodialWalletManager
) : TxEngine() {

    override fun assertInputsValid() {
        check(txTarget is CryptoAddress)
        check(sourceAsset == (txTarget as CryptoAddress).asset)
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        walletManager.fetchCryptoWithdrawFeeAndMinLimit(sourceAsset)
            .map {
                PendingTx(
                    amount = CryptoValue.zero(sourceAsset),
                    totalBalance = CryptoValue.zero(sourceAsset),
                    availableBalance = CryptoValue.zero(sourceAsset),
                    feeForFullAvailable = CryptoValue.zero(sourceAsset),
                    feeAmount = CryptoValue.fromMinor(sourceAsset, it.fee),
                    feeSelection = FeeSelection(),
                    selectedFiat = userFiat,
                    minLimit = CryptoValue.fromMinor(sourceAsset, it.minLimit)
                )
            }

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        require(amount is CryptoValue)
        require(amount.currency == sourceAsset)

        return Singles.zip(
            sourceAccount.accountBalance.map { it as CryptoValue },
            sourceAccount.actionableBalance.map { it as CryptoValue }
        ) { total, available ->
            pendingTx.copy(
                amount = amount,
                totalBalance = total,
                availableBalance = available
            )
        }
    }

    override fun doUpdateFeeLevel(
        pendingTx: PendingTx,
        level: FeeLevel,
        customFeeAmount: Long
    ): Single<PendingTx> {
        require(pendingTx.feeSelection.availableLevels.contains(level))
        // This engine only supports FeeLevel.None, so
        return Single.just(pendingTx)
    }

    private fun buildNewConfirmation(pendingTx: PendingTx): PendingTx =
        pendingTx.copy(
            confirmations = listOfNotNull(
                TxConfirmationValue.NewFrom(sourceAccount, sourceAsset),
                TxConfirmationValue.NewTo(
                    txTarget, AssetAction.Send, sourceAccount
                ),
                TxConfirmationValue.CompoundNetworkFee(
                    receivingFeeInfo = if (!pendingTx.feeAmount.isZero) {
                        FeeInfo(
                            pendingTx.feeAmount,
                            pendingTx.feeAmount.toFiat(exchangeRates, userFiat),
                            sourceAsset
                        )
                    } else null,
                    feeLevel = pendingTx.feeSelection.selectedLevel,
                    ignoreErc20LinkedNote = true
                ),
                TxConfirmationValue.NewTotal(
                    totalWithFee = (pendingTx.amount as CryptoValue).plus(
                        pendingTx.feeAmount as CryptoValue
                    ),
                    exchange = pendingTx.amount.toFiat(exchangeRates, userFiat)
                        .plus(pendingTx.feeAmount.toFiat(exchangeRates, userFiat))
                ),
                if (isNoteSupported) {
                    TxConfirmationValue.Description()
                } else null
            )
        )

    private fun buildOldConfirmation(pendingTx: PendingTx): PendingTx =
        pendingTx.copy(confirmations = listOf(
            TxConfirmationValue.From(from = sourceAccount.label),
            TxConfirmationValue.To(to = txTarget.label),
            TxConfirmationValue.FeedTotal(amount = pendingTx.amount, fee = pendingTx.feeAmount)
        ).apply {
            if (isNoteSupported) {
                toMutableList().add(TxConfirmationValue.Description())
            }
        })

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            if (internalFeatureFlagApi.isFeatureEnabled(GatedFeature.CHECKOUT)) {
                buildNewConfirmation(pendingTx)
            } else {
                buildOldConfirmation(pendingTx)
            }
        )

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx).updateTxValidity(pendingTx)

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx).updateTxValidity(pendingTx)

    private fun validateAmounts(pendingTx: PendingTx): Completable =
        sourceAccount.actionableBalance
            .flatMapCompletable { max ->
                val min = pendingTx.minLimit ?: CryptoValue.zero(sourceAsset)
                if (pendingTx.amount.isPositive && max >= pendingTx.amount && min <= pendingTx.amount) {
                    Completable.complete()
                } else {
                    throw TxValidationFailure(
                        if (pendingTx.amount > pendingTx.availableBalance) {
                            ValidationState.INSUFFICIENT_FUNDS
                        } else {
                            ValidationState.INVALID_AMOUNT
                        }
                    )
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
