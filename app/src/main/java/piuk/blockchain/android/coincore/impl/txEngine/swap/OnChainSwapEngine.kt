package piuk.blockchain.android.coincore.impl.txEngine.swap

import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.TransferDirection
import com.blockchain.nabu.datamanagers.repositories.QuotesProvider
import com.blockchain.nabu.service.TierService
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.Single
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.impl.txEngine.OnChainTxEngineBase
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class OnChainSwapEngine(
    quotesProvider: QuotesProvider,
    walletManager: CustodialWalletManager,
    tiersService: TierService,
    override val direction: TransferDirection,
    private val engine: OnChainTxEngineBase,
    environmentConfig: EnvironmentConfig
) : SwapEngineBase(
    quotesProvider, walletManager, tiersService, environmentConfig
) {

    override val availableBalance: Single<Money>
        get() = sourceAccount.accountBalance

    override fun doInitialiseTx(): Single<PendingTx> {
        return quotesEngine.pricedQuote.firstOrError().doOnSuccess { pricedQuote ->
            engine.startFromQuote(pricedQuote)
        }.flatMap { quote ->
            engine.doInitialiseTx().flatMap {
                updateLimits(it, quote)
            }
        }.map { px ->
            px.copy(feeLevel = FeeLevel.Priority)
        }.handlePendingOrdersError(
            PendingTx(
                amount = CryptoValue.zero(sourceAccount.asset),
                available = CryptoValue.zero(sourceAccount.asset),
                fees = CryptoValue.ZeroBch,
                selectedFiat = userFiat)
        )
    }

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        return engine.doUpdateAmount(amount, pendingTx).updateQuotePrice().clearConfirmations()
    }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> {
        return engine.doValidateAmount(pendingTx).flatMap {
            if (
                it.validationState == ValidationState.CAN_EXECUTE ||
                it.validationState == ValidationState.INVALID_AMOUNT
            ) {
                super.doValidateAmount(pendingTx)
            } else {
                Single.just(it)
            }
        }.updateTxValidity(pendingTx)
    }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> {
        return engine.doValidateAll(pendingTx).flatMap {
            if (
                it.validationState == ValidationState.CAN_EXECUTE ||
                it.validationState == ValidationState.INVALID_AMOUNT
            ) {
                super.doValidateAll(pendingTx)
            } else {
                Single.just(it)
            }
        }.updateTxValidity(pendingTx)
    }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> {
        return createOrder(pendingTx).flatMap { order ->
            engine.restartFromOrder(order, pendingTx).flatMap { px ->
                engine.doExecute(px, secondPassword).updateOrderStatus(order.id)
            }
        }
    }
}