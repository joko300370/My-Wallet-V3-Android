package piuk.blockchain.android.coincore.impl.txEngine.interest

import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FeeSelection
import piuk.blockchain.android.coincore.InterestAccount
import piuk.blockchain.android.coincore.NonCustodialAccount
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.updateTxValidity

class InterestWithdrawOnChainTxEngine(
    private val walletManager: CustodialWalletManager
) : InterestBaseEngine(walletManager) {

    private val availableBalance: Single<Money>
        get() = sourceAccount.accountBalance

    override fun assertInputsValid() {
        check(sourceAccount is InterestAccount)
        check(txTarget is CryptoAccount)
        check(txTarget is NonCustodialAccount)
        check(sourceAsset == (txTarget as CryptoAccount).asset)
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        Singles.zip(
            walletManager.fetchCryptoWithdrawFeeAndMinLimit(sourceAsset, Product.SAVINGS),
            walletManager.getInterestLimits(sourceAsset).toSingle(),
            availableBalance
        ).map { (minLimits, maxLimits, balance) ->
            PendingTx(
                amount = CryptoValue.zero(sourceAsset),
                minLimit = CryptoValue.fromMinor(sourceAsset, minLimits.minLimit),
                maxLimit = maxLimits.maxWithdrawalAmount,
                feeSelection = FeeSelection(),
                selectedFiat = userFiat,
                availableBalance = balance,
                totalBalance = balance,
                feeAmount = CryptoValue.fromMinor(sourceAsset, minLimits.fee),
                feeForFullAvailable = CryptoValue.zero(sourceAsset)
            )
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

    private fun checkIfAmountIsBelowMinLimit(pendingTx: PendingTx) =
        when {
            pendingTx.minLimit == null -> {
                throw TxValidationFailure(ValidationState.UNINITIALISED)
            }
            pendingTx.amount < pendingTx.minLimit -> throw TxValidationFailure(ValidationState.UNDER_MIN_LIMIT)
            else -> Completable.complete()
        }

    override fun doUpdateFeeLevel(pendingTx: PendingTx, level: FeeLevel, customFeeAmount: Long): Single<PendingTx> =
        Single.just(pendingTx)

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        availableBalance.flatMapCompletable { balance ->
            if (pendingTx.amount <= balance) {
                checkIfAmountIsBelowMinLimit(pendingTx)
            } else {
                throw TxValidationFailure(ValidationState.INSUFFICIENT_FUNDS)
            }
        }.updateTxValidity(pendingTx)

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            pendingTx.copy(
                confirmations = listOfNotNull(
                    TxConfirmationValue.NewFrom(sourceAccount, sourceAsset),
                    TxConfirmationValue.NewTo(
                        txTarget, AssetAction.InterestDeposit, sourceAccount
                    ),
                    TxConfirmationValue.NewNetworkFee(
                        pendingTx.feeAmount,
                        pendingTx.feeAmount.toFiat(exchangeRates, userFiat),
                        sourceAsset
                    ),
                    TxConfirmationValue.NewTotal(
                        totalWithFee = (pendingTx.amount as CryptoValue).plus(
                            pendingTx.feeAmount as CryptoValue
                        ),
                        exchange = pendingTx.amount.toFiat(exchangeRates, userFiat)
                            .plus(pendingTx.feeAmount.toFiat(exchangeRates, userFiat))
                    )
                )
            )
        )

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        doValidateAmount(pendingTx)

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        (txTarget as CryptoAccount).receiveAddress.flatMap {
            walletManager.startInterestWithdrawal(sourceAsset, pendingTx.amount, it.address).toSingle {
                TxResult.UnHashedTxResult(pendingTx.amount)
            }
        }
}
