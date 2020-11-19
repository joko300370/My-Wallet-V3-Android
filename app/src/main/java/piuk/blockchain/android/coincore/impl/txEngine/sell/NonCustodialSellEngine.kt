package piuk.blockchain.android.coincore.impl.txEngine.sell

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.TransferDirection
import com.blockchain.swap.nabu.datamanagers.repositories.QuotesProvider
import com.blockchain.swap.nabu.service.TierService
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

class NonCustodialSellEngine(
    private val engine: OnChainTxEngineBase,
    environmentConfig: EnvironmentConfig,
    walletManager: CustodialWalletManager,
    kycTierService: TierService,
    quotesProvider: QuotesProvider
) : SellTxEngine(
    walletManager, kycTierService, quotesProvider, environmentConfig
) {
    override val direction: TransferDirection
        get() = TransferDirection.FROM_USERKEY

    override fun doInitialiseTx(): Single<PendingTx> =
        quotesEngine.pricedQuote.firstOrError().doOnSuccess { pricedQuote ->
            engine.startFromQuote(pricedQuote)
        }.flatMap { quote ->
            engine.doInitialiseTx().flatMap {
                updateLimits(it, quote)
            }
        }.map { px ->
            px.copy(feeLevel = FeeLevel.Priority, selectedFiat = userFiat)
        }.handlePendingOrdersError(
            PendingTx(
                amount = CryptoValue.zero(sourceAccount.asset),
                available = CryptoValue.zero(sourceAccount.asset),
                fees = CryptoValue.ZeroBch,
                selectedFiat = userFiat
            )
        )

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

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        return engine.doUpdateAmount(amount, pendingTx).updateQuotePrice().clearConfirmations()
    }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> {
        return createSellOrder(pendingTx).flatMap { order ->
            engine.restartFromOrder(order, pendingTx).flatMap { px ->
                engine.doExecute(px, secondPassword).updateOrderStatus(order.id)
            }
        }
    }
}