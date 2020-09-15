package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.koin.scopedInject
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Single
import org.koin.core.KoinComponent
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.TxOption
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

class InterestDepositTxEngine(
    private val onChainTxEngine: OnChainTxEngineBase
) : TxEngine(), KoinComponent {

    private val custodialWalletManager: CustodialWalletManager by scopedInject()

    override fun start(
        sourceAccount: CryptoAccount,
        txTarget: TransactionTarget,
        exchangeRates: ExchangeRateDataManager
    ) {
        super.start(sourceAccount, txTarget, exchangeRates)
        onChainTxEngine.start(sourceAccount, txTarget, exchangeRates)
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        onChainTxEngine.doInitialiseTx()
            .flatMap { pendingTx ->
                custodialWalletManager.getInterestLimits(asset).toSingle().map {
                    pendingTx.copy(minLimit = it.minDepositAmount)
                }
            }

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> =
        onChainTxEngine.doUpdateAmount(amount, pendingTx)

    override val feeOptions: Set<FeeLevel>
        get() = onChainTxEngine.feeOptions

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            pendingTx.copy(
                options = listOf(
                    TxOptionValue.From(from = sourceAccount.label),
                    TxOptionValue.To(to = txTarget.label),
                    TxOptionValue.Fee(fee = pendingTx.fees),
                    TxOptionValue.FeedTotal(amount = pendingTx.amount, fee = pendingTx.fees),
                    TxOptionValue.TxBooleanOption<Unit>(
                        _option = TxOption.AGREEMENT_INTEREST_T_AND_C
                    ),
                    TxOptionValue.TxBooleanOption(
                        _option = TxOption.AGREEMENT_INTEREST_TRANSFER,
                        data = pendingTx.amount
                    )
                )
            )
        )

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        onChainTxEngine.doValidateAmount(pendingTx)
            .map {
                if (it.amount.isPositive && it.amount < it.minLimit!!) {
                    it.copy(validationState = ValidationState.UNDER_MIN_LIMIT)
                } else {
                    it
                }
            }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        onChainTxEngine.doValidateAll(pendingTx)
            .map {
                if (it.validationState == ValidationState.CAN_EXECUTE && !areOptionsValid(pendingTx)) {
                    it.copy(validationState = ValidationState.OPTION_INVALID)
                } else {
                    it
                }
            }

    private fun areOptionsValid(pendingTx: PendingTx): Boolean {
        val terms = pendingTx.getOption<TxOptionValue.TxBooleanOption<Unit>>(
            TxOption.AGREEMENT_INTEREST_T_AND_C
        )?.value ?: false
        val transfer = pendingTx.getOption<TxOptionValue.TxBooleanOption<Money>>(
            TxOption.AGREEMENT_INTEREST_TRANSFER
        )?.value ?: false
        return (terms && transfer)
    }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Completable =
        onChainTxEngine.doExecute(pendingTx, secondPassword)
}
