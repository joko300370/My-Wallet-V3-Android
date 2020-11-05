package piuk.blockchain.android.coincore.impl.txEngine.swap

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.SwapDirection
import com.blockchain.swap.nabu.datamanagers.repositories.QuotesProvider
import com.blockchain.swap.nabu.service.TierService
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.impl.makeExternalAssetAddress
import piuk.blockchain.android.coincore.impl.txEngine.OnChainTxEngineBase
import piuk.blockchain.android.coincore.impl.txEngine.PricedQuote
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.utils.extensions.thenSingle
import java.lang.IllegalStateException

class OnChainSwapEngine(
    private val quotesProvider: QuotesProvider,
    walletManager: CustodialWalletManager,
    tiersService: TierService,
    override val direction: SwapDirection,
    private val engine: OnChainTxEngineBase,
    private val environmentConfig: EnvironmentConfig,
    private val custodialWalletManager: CustodialWalletManager
) : SwapEngineBase(
    quotesProvider, walletManager, tiersService
) {

    override fun doInitialiseTx(): Single<PendingTx> {
        return quotesEngine.pricedQuote.firstOrError().doOnSuccess { pricedQuote ->
            startOnChainEngine(pricedQuote)
        }.flatMap { quote ->
            engine.doInitialiseTx().flatMap {
                updateLimits(it, quote)
            }
        }.map { px ->
            px.copy(feeLevel = FeeLevel.Priority)
        }
    }

    private fun startOnChainEngine(pricedQuote: PricedQuote) {
        engine.start(
            sourceAccount = sourceAccount,
            txTarget = makeExternalAssetAddress(
                asset = sourceAccount.asset,
                address = pricedQuote.swapQuote.sampleDepositAddress,
                label = pricedQuote.swapQuote.sampleDepositAddress,
                environmentConfig = environmentConfig,
                postTransactions = { Completable.complete() }
            ),
            exchangeRates = exchangeRates
        )
    }

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        return engine.doUpdateAmount(amount, pendingTx).updateQuotePrice()
    }

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> {
        return super.doBuildConfirmations(pendingTx).map {
            val updatedList = it.confirmations.toMutableList()
            updatedList.add(
                TxConfirmationValue.NetworkFee(
                    fee = pendingTx.fees,
                    type = TxConfirmationValue.NetworkFee.FeeType.DEPOSIT_FEE,
                    asset = sourceAccount.asset
                )
            )

            it.copy(
                confirmations = updatedList.toList()
            )
        }
    }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> {
        return engine.doValidateAmount(pendingTx).flatMap { super.doValidateAmount(pendingTx) }
            .updateTxValidity(pendingTx)
    }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> {
        return engine.doValidateAll(pendingTx).flatMap { super.doValidateAll(pendingTx) }
            .updateTxValidity(pendingTx)
    }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> {
        return createOrder(pendingTx).flatMap { order ->
            engine.restart(
                txTarget = makeExternalAssetAddress(
                    asset = sourceAccount.asset,
                    address = order.depositAddress ?: throw IllegalStateException("Missing deposit address"),
                    label = order.depositAddress ?: throw IllegalStateException("Missing deposit address"),
                    environmentConfig = environmentConfig,
                    postTransactions = { Completable.complete() }
                ),
                pendingTx = pendingTx
            ).flatMap { px ->
                engine.doExecute(px, secondPassword)
                    .onErrorResumeNext { error ->
                        custodialWalletManager.updateSwapOrder(order.id, false).onErrorComplete().toSingle {
                            throw error
                        }
                    }
                    .flatMap { result ->
                        custodialWalletManager.updateSwapOrder(order.id, true).onErrorComplete().thenSingle {
                            Single.just(result)
                        }
                    }
            }
        }
    }
}