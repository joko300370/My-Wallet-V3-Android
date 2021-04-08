package piuk.blockchain.android.coincore.impl.txEngine.interest

import com.blockchain.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.TxConfirmation
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.android.coincore.impl.txEngine.OnChainTxEngineBase
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

class InterestDepositOnChainTxEngine(
    walletManager: CustodialWalletManager,
    private val onChainEngine: OnChainTxEngineBase
) : InterestEngine(walletManager) {

    override fun assertInputsValid() {
        check(sourceAccount is CryptoNonCustodialAccount)
        check(txTarget is ReceiveAddress)

        // TODO: Re-enable this once start() has been refactored to be Completable
        // We have to pass the receiveAddress here cause we need to start the onchain engine with that
        // and so we need a way to get the receiveAddress from the CryptoInterestAccount.
        // This will be possible when start() returns a completable
        // check(sourceAccount.asset == (txTarget as CryptoInterestAccount).asset)
        // check(txTarget is CryptoInterestAccount)
        // onChainEngine.assertInputsValid()
    }

    override fun start(
        sourceAccount: BlockchainAccount,
        txTarget: TransactionTarget,
        exchangeRates: ExchangeRateDataManager,
        refreshTrigger: RefreshTrigger
    ) {
        super.start(sourceAccount, txTarget, exchangeRates, refreshTrigger)
        onChainEngine.start(sourceAccount, txTarget, exchangeRates, refreshTrigger)
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        onChainEngine.doInitialiseTx()
            .flatMap { pendingTx ->
                getLimits()
                    .map {
                        pendingTx.copy(
                            minLimit = it.minDepositAmount,
                            feeSelection = pendingTx.feeSelection.copy(
                                selectedLevel = FeeLevel.Regular,
                                availableLevels = AVAILABLE_FEE_LEVELS
                            )
                        )
                    }
            }

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> =
        onChainEngine.doUpdateAmount(amount, pendingTx)

    override fun doUpdateFeeLevel(
        pendingTx: PendingTx,
        level: FeeLevel,
        customFeeAmount: Long
    ): Single<PendingTx> {
        require(pendingTx.feeSelection.availableLevels.contains(level))
        return Single.just(pendingTx)
    }

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        onChainEngine.doBuildConfirmations(pendingTx).map { pTx ->
            modifyEngineConfirmations(pTx)
        }.flatMap {
            if (it.hasOption(TxConfirmation.MEMO)) {
                it.getOption<TxConfirmationValue.Memo>(TxConfirmation.MEMO)?.let { memo ->
                    onChainEngine.doOptionUpdateRequest(it, memo.copy(editable = false))
                }
            } else {
                Single.just(it)
            }
        }

    override fun doOptionUpdateRequest(pendingTx: PendingTx, newConfirmation: TxConfirmationValue): Single<PendingTx> =
        if (newConfirmation.confirmation in setOf(
                TxConfirmation.AGREEMENT_INTEREST_T_AND_C,
                TxConfirmation.AGREEMENT_INTEREST_TRANSFER
            )
        ) {
            Single.just(pendingTx.addOrReplaceOption(newConfirmation))
        } else {
            onChainEngine.doOptionUpdateRequest(pendingTx, newConfirmation)
                .map { pTx ->
                    modifyEngineConfirmations(
                        pendingTx = pTx
                    )
                }
        }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        onChainEngine.doValidateAmount(pendingTx)
            .map {
                if (it.amount.isPositive && it.amount < it.minLimit!!) {
                    it.copy(validationState = ValidationState.UNDER_MIN_LIMIT)
                } else {
                    it
                }
            }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        onChainEngine.doValidateAll(pendingTx)
            .map {
                if (it.validationState == ValidationState.CAN_EXECUTE && !areOptionsValid(pendingTx)) {
                    it.copy(validationState = ValidationState.OPTION_INVALID)
                } else {
                    it
                }
            }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        onChainEngine.doExecute(pendingTx, secondPassword)

    override fun doPostExecute(txResult: TxResult): Completable = txTarget.onTxCompleted(txResult)

    companion object {
        private val AVAILABLE_FEE_LEVELS = setOf(FeeLevel.Regular)
    }
}
