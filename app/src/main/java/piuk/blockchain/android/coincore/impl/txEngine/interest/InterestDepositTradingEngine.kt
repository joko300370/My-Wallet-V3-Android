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
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.TradingAccount
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount

class InterestDepositTradingEngine(private val walletManager: CustodialWalletManager) : TxEngine() {

    override fun assertInputsValid() {
        check(sourceAccount is TradingAccount)
        check(txTarget is InterestAccount)
    }

    private val availableBalance: Single<Money>
        get() = sourceAccount.accountBalance

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> {
        return Single.just(pendingTx)
    }

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
        )
    }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> {
        return Single.just(pendingTx)
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