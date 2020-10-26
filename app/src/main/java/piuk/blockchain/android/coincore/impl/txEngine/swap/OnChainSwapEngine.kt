package piuk.blockchain.android.coincore.impl.txEngine.swap

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.Direction
import com.blockchain.swap.nabu.datamanagers.repositories.QuotesProvider
import com.blockchain.swap.nabu.service.TierService
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.impl.makeExternalAssetAddress
import piuk.blockchain.android.coincore.impl.txEngine.OnChainTxEngineBase
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class OnChainSwapEngine(
    isNoteSupported: Boolean,
    private val quotesProvider: QuotesProvider,
    walletManager: CustodialWalletManager,
    tiersService: TierService,
    override val direction: Direction,
    private val engine: OnChainTxEngineBase,
    private val environmentConfig: EnvironmentConfig
) : SwapEngineBase(
    isNoteSupported, quotesProvider, walletManager, tiersService
) {

    private val refreshTrigger = object : RefreshTrigger {
        override fun refreshConfirmations(revalidate: Boolean): Completable {
            // TODO for swap quote updates
            return Completable.complete()
        }
    }

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
                exchangeRates = exchangeRates,
                refreshTrigger = refreshTrigger
            )
        }.flatMap {
            super.doInitialiseTx()
        }.flatMap {
            engine.doInitialiseTx()
        }
    }

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        return super.doUpdateAmount(amount, pendingTx).flatMap { engine.doUpdateAmount(amount, pendingTx) }
    }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> {
        return super.doValidateAmount(pendingTx).flatMap { engine.doValidateAmount(pendingTx) }
    }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> {
        return engine.doValidateAll(pendingTx)
    }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> {
        return createOrder(pendingTx).flatMap {
            engine.doExecute(pendingTx, secondPassword)
        }
    }
}