package piuk.blockchain.android.coincore.impl.txEngine.sell

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.TransferDirection
import com.blockchain.swap.nabu.datamanagers.repositories.QuotesProvider
import com.blockchain.swap.nabu.service.TierService
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxResult

open class CustodialSellTxEngine(
    private val walletManager: CustodialWalletManager,
    private val quotesProvider: QuotesProvider,
    private val kycTierService: TierService
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
            }
    }

    override val requireSecondPassword: Boolean
        get() = false

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> {
        val latestQuoteExchangeRate =
            ExchangeRate.CryptoToFiat(
                from = sourceAccount.asset,
                to = userFiat,
                _rate = quotesEngine.getLatestQuote().price.toBigDecimal()
            )

        return Single.just(
            pendingTx.copy(
                confirmations = listOf(
                    TxConfirmationValue.ExchangePriceConfirmation(quotesEngine.getLatestQuote().price,
                        sourceAccount.asset),
                    TxConfirmationValue.From(sourceAccount.label),
                    TxConfirmationValue.To(userFiat),
                    TxConfirmationValue.Total(if (pendingTx.amount is FiatValue) pendingTx.amount else
                        latestQuoteExchangeRate.convert(pendingTx.amount)
                    )
                ))
        )
    }

    /*  private fun updateOptionFromQuote(quote: CustodialQuote, pendingTx: PendingTx): PendingTx {

          val options = listOf(
              TxConfirmationValue.ExchangePriceConfirmation(quote.rate, sourceAccount.asset),
              TxConfirmationValue.From(sourceAccount.label),
              TxConfirmationValue.To(fiatTarget.label),
              TxConfirmationValue.Total(if (pendingTx.amount is FiatValue) pendingTx.amount else
                  FiatValue.fromMajor(userFiat,
                      pendingTx.amount.toBigDecimal().times(quote.rate.toBigDecimal()))
              )
          )

          val exchangeRate = ExchangeRate.FiatToCrypto(
              from = fiatTarget.fiatCurrency,
              to = sourceAccount.asset,
              rate = 1.toBigDecimal().divide(quote.rate.toBigDecimal(),
                  sourceAccount.asset.dp, RoundingMode.HALF_UP
              )
          )

          return pendingTx.copy(
              confirmations = options,
              minLimit = if (pendingTx.minLimit is CryptoValue) pendingTx.minLimit else
                  exchangeRate.convert(pendingTx.minLimit ?: FiatValue.zero(fiatTarget.fiatCurrency)),
              maxLimit = if (pendingTx.maxLimit is CryptoValue) pendingTx.maxLimit else
                  exchangeRate.convert(pendingTx.maxLimit ?: FiatValue.zero(fiatTarget.fiatCurrency))
          )
      }*/

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        walletManager.createSwapOrder(
            direction = direction,
            quoteId = quotesEngine.getLatestQuote().transferQuote.id,
            volume = pendingTx.amount
        ).map {
            TxResult.UnHashedTxResult(pendingTx.amount) as TxResult
        }
}