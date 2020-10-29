package piuk.blockchain.android.coincore.impl.txEngine.swap

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.SwapDirection
import com.blockchain.swap.nabu.datamanagers.repositories.QuotesProvider
import com.blockchain.swap.nabu.service.TierService
import io.reactivex.Single
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.impl.CustodialTradingAccount

class TradingToTradingSwapTxEngine(
    isNoteSupported: Boolean,
    walletManager: CustodialWalletManager,
    quotesProvider: QuotesProvider,
    kycTierService: TierService
) : SwapEngineBase(isNoteSupported, quotesProvider, walletManager, kycTierService) {

    override fun assertInputsValid() {
        require(txTarget is CustodialTradingAccount)
        require(sourceAccount is CustodialTradingAccount)
        require((txTarget as CustodialTradingAccount).asset != sourceAccount.asset)
    }

    override val direction: SwapDirection
        get() = SwapDirection.INTERNAL

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        createOrder(pendingTx).map {
            TxResult.UnHashedTxResult(pendingTx.amount)
        }
}
