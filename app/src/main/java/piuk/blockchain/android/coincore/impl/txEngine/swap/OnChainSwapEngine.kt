package piuk.blockchain.android.coincore.impl.txEngine.swap

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.SwapDirection
import com.blockchain.swap.nabu.datamanagers.repositories.QuotesProvider
import com.blockchain.swap.nabu.service.TierService
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.impl.makeExternalAssetAddress
import piuk.blockchain.android.coincore.impl.txEngine.OnChainTxEngineBase
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class OnChainSwapEngine(
    isNoteSupported: Boolean,
    private val quotesProvider: QuotesProvider,
    walletManager: CustodialWalletManager,
    tiersService: TierService,
    override val direction: SwapDirection,
    private val engine: OnChainTxEngineBase,
    private val environmentConfig: EnvironmentConfig
) : SwapEngineBase(
    isNoteSupported, quotesProvider, walletManager, tiersService
) {

    override fun doInitialiseTx(): Single<PendingTx> {
        return quotesEngine.quote.firstOrError().doOnSuccess { quote ->
            engine.start(
                sourceAccount = sourceAccount,
                txTarget = makeExternalAssetAddress(
                    asset = sourceAccount.asset,
                    address = quote.sampleDepositAddress,
                    label = quote.sampleDepositAddress,
                    environmentConfig = environmentConfig,
                    postTransactions = { Completable.complete() }
                ),
                exchangeRates = exchangeRates
            )
        }.flatMap {
            engine.doInitialiseTx()
        }.flatMap {
            updateLimits(it)
        }
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
        return createOrder(pendingTx).flatMap {
            engine.doExecute(pendingTx, secondPassword)
        }
    }
}