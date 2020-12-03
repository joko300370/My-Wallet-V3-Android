package piuk.blockchain.android.coincore.impl.txEngine.swap

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
import piuk.blockchain.android.coincore.impl.CustodialTradingAccount
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class TradingToTradingSwapTxEngine(
    walletManager: CustodialWalletManager,
    quotesProvider: QuotesProvider,
    kycTierService: TierService,
    environmentConfig: EnvironmentConfig
) : SwapEngineBase(quotesProvider, walletManager, kycTierService, environmentConfig) {

    override fun assertInputsValid() {
        require(txTarget is CustodialTradingAccount)
        require(sourceAccount is CustodialTradingAccount)
        require((txTarget as CustodialTradingAccount).asset != sourceAccount.asset)
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        quotesEngine.pricedQuote.firstOrError().flatMap { pricedQuote ->
            sourceAccount.actionableBalance.flatMap { balance ->
                Single.just(PendingTx(
                    amount = CryptoValue.zero(sourceAccount.asset),
                    available = balance,
                    fees = CryptoValue.zero(sourceAccount.asset),
                    feeLevel = FeeLevel.None,
                    selectedFiat = userFiat)).flatMap {
                    updateLimits(it, pricedQuote)
                }
            }
        }.handlePendingOrdersError(
            PendingTx(
                amount = CryptoValue.zero(sourceAccount.asset),
                available = CryptoValue.zero(target.asset),
                fees = CryptoValue.ZeroBch,
                selectedFiat = userFiat
            )
        )

    override val direction: TransferDirection
        get() = TransferDirection.INTERNAL

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        createOrder(pendingTx).map {
            TxResult.UnHashedTxResult(pendingTx.amount)
        }

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> =
        sourceAccount.actionableBalance.map { balance -> balance as CryptoValue }.map { available ->
            pendingTx.copy(
                amount = amount,
                available = available
            )
        }.updateQuotePrice().clearConfirmations()
}
