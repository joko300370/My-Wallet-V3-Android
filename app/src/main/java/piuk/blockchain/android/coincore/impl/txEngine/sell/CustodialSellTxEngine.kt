package piuk.blockchain.android.coincore.impl.txEngine.sell

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.TransferDirection
import com.blockchain.swap.nabu.datamanagers.repositories.QuotesProvider
import com.blockchain.swap.nabu.service.TierService
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxResult

class CustodialSellTxEngine(
    private val walletManager: CustodialWalletManager,
    quotesProvider: QuotesProvider,
    kycTierService: TierService
) : SellTxEngine(walletManager, kycTierService, quotesProvider) {

    override val direction: TransferDirection
        get() = TransferDirection.INTERNAL

    override fun doInitialiseTx(): Single<PendingTx> =
        quotesEngine.pricedQuote.firstOrError()
            .zipWith(sourceAccount.accountBalance).flatMap { (quote, balance) ->
                Single.just(
                    PendingTx(
                        amount = FiatValue.zero(userFiat),
                        available = balance,
                        fees = CryptoValue.zero(sourceAccount.asset),
                        selectedFiat = userFiat,
                        feeLevel = FeeLevel.None
                    )
                ).flatMap {
                    updateLimits(it, quote)
                }
            }.handlePendingOrdersError(
                PendingTx(
                    amount = CryptoValue.zero(sourceAccount.asset),
                    available = CryptoValue.zero(sourceAccount.asset),
                    fees = CryptoValue.zero(sourceAccount.asset),
                    selectedFiat = userFiat
                )
            )

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        return sourceAccount.accountBalance
            .map { it as CryptoValue }
            .map { available ->
                pendingTx.copy(
                    amount = amount,
                    available = available
                )
            }.updateQuotePrice().clearConfirmations()
    }

    override val requireSecondPassword: Boolean
        get() = false

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        walletManager.createCustodialOrder(
            direction = direction,
            quoteId = quotesEngine.getLatestQuote().transferQuote.id,
            volume = pendingTx.amount
        ).map {
            TxResult.UnHashedTxResult(pendingTx.amount) as TxResult
        }.doFinally {
            disposeQuotesFetching(pendingTx)
        }
}