package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.swap.nabu.datamanagers.CurrencyPair
import com.blockchain.swap.nabu.datamanagers.CustodialQuote
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.OrderInput
import com.blockchain.swap.nabu.datamanagers.OrderOutput
import com.blockchain.swap.nabu.datamanagers.TransferDirection
import com.blockchain.swap.nabu.datamanagers.repositories.QuotesProvider
import com.blockchain.swap.nabu.models.simplebuy.CustodialWalletOrder
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.toCrypto
import piuk.blockchain.androidcore.utils.extensions.thenSingle
import java.lang.IllegalStateException
import java.math.RoundingMode

open class CustodialSellTxEngine(
    private val walletManager: CustodialWalletManager,
    private val quotesProvider: QuotesProvider
) : TxEngine() {

    // The reason that we are using these values is that the API responds with fiatvalues as limits, so when
    // user swaps to crypto we convert the limits to Crypto if we swap again we convert these values again to fiat and
    // sometimes due to rounding there are values like 4.99 or 5.01 displayed in the UI
    // instead of 5 that was originally returned from the API.

    private lateinit var minApiFiatAmount: FiatValue
    private lateinit var maxApiFiatAmount: FiatValue

    private val fiatTarget: FiatAccount by lazy {
        txTarget as FiatAccount
    }

    override val userFiat: String by lazy {
        fiatTarget.fiatCurrency
    }

    private lateinit var quotesEngine: TransferQuotesEngine

    private val cryptoCurrency: CryptoCurrency
        get() = sourceAccount.asset

    private val direction: TransferDirection
        get() = TransferDirection.INTERNAL

    private val pair: CurrencyPair.CryptoToFiatCurrencyPair
        get() = CurrencyPair.CryptoToFiatCurrencyPair(sourceAccount.asset, userFiat)

    override fun start(sourceAccount: CryptoAccount, txTarget: TransactionTarget, exchangeRates: ExchangeRateDataManager, refreshTrigger: RefreshTrigger) {
        super.start(sourceAccount, txTarget, exchangeRates, refreshTrigger)
        quotesEngine = TransferQuotesEngine(quotesProvider, direction, pair)
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        walletManager.cancelAllPendingOrders()
            .onErrorComplete()
            .thenSingle {
                walletManager.getSupportedBuySellCryptoCurrencies(fiatTarget.fiatCurrency)
                    .map {
                        it.pairs.first { pair ->
                            pair.cryptoCurrency == sourceAccount.asset &&
                                    pair.fiatCurrency == fiatTarget.fiatCurrency
                        }
                    }.flatMap { pair ->
                        quotesEngine.pricedQuote.firstOrError().map { it ->
                            PendingTx(
                                amount = FiatValue.zero(fiatTarget.fiatCurrency),
                                available = CryptoValue.zero(sourceAccount.asset),
                                fees = CryptoValue.zero(sourceAccount.asset),
                                selectedFiat = userFiat,
                                maxLimit = pair.sellLimits.maxLimit(fiatTarget.fiatCurrency),
                                minLimit = pair.sellLimits.minLimit(fiatTarget.fiatCurrency),
                                feeLevel = FeeLevel.None
                            )
                        }
                    }.doOnSuccess {
                        minApiFiatAmount = it.minLimit as FiatValue
                        maxApiFiatAmount = it.maxLimit as FiatValue
                    }
            }

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        return sourceAccount.accountBalance
            .map { it as CryptoValue }
            .map { available ->
                pendingTx.copy(
                    amount = amount,
                    available = available,
                    maxLimit = maxLimit(amount, pendingTx),
                    minLimit = minLimit(amount, pendingTx)
                )
            }
    }

    private fun maxLimit(amount: Money, pendingTx: PendingTx): Money? =
        when (amount) {
            is FiatValue -> maxApiFiatAmount
            is CryptoValue -> (pendingTx.maxLimit as? FiatValue)?.let {
                it.toCrypto(exchangeRates, amount.currency)
            } ?: pendingTx.maxLimit
            else -> throw IllegalStateException("Unknown money type")
        }

    private fun minLimit(amount: Money, pendingTx: PendingTx): Money? =
        when (amount) {
            is FiatValue -> minApiFiatAmount
            is CryptoValue -> (pendingTx.minLimit as? FiatValue)?.let {
                it.toCrypto(exchangeRates, amount.currency)
            } ?: pendingTx.minLimit
            else -> throw IllegalStateException("Unknown money type")
        }

    override val requireSecondPassword: Boolean
        get() = false

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx).updateTxValidity(pendingTx)

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx).updateTxValidity(pendingTx)

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
                    TxConfirmationValue.To(fiatTarget.label),
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

    private fun validateAmounts(pendingTx: PendingTx): Completable =
        sourceAccount.accountBalance.map { it as CryptoValue }
            .flatMapCompletable { balance ->
                val cryptoAmount =
                    (pendingTx.amount as? FiatValue)?.toCrypto(exchangeRates, cryptoCurrency)
                        ?: pendingTx.amount as CryptoValue

                val maxLimitCrypto =
                    (pendingTx.maxLimit as? FiatValue)?.toCrypto(exchangeRates, cryptoCurrency)
                        ?: pendingTx.maxLimit as? CryptoValue ?: CryptoValue.zero(cryptoCurrency)

                val maxAvailable = Money.min(balance,
                    (pendingTx.maxLimit as? FiatValue)?.toCrypto(exchangeRates, cryptoCurrency)
                        ?: pendingTx.maxLimit as CryptoValue
                        ?: CryptoValue.zero(cryptoCurrency)) as CryptoValue

                val minAvailable =
                    (pendingTx.minLimit as? FiatValue)?.toCrypto(exchangeRates, cryptoCurrency)
                        ?: pendingTx.minLimit as CryptoValue
                        ?: CryptoValue.zero(cryptoCurrency)

                if ((maxAvailable >= cryptoAmount && minAvailable <= cryptoAmount)) {
                    Completable.complete()
                } else {
                    throw txValidationFailure(cryptoAmount,
                        maxLimitCrypto,
                        minAvailable,
                        balance
                    )
                }
            }

    private fun txValidationFailure(
        amount: CryptoValue,
        maxAvailable: CryptoValue,
        minAvailable: CryptoValue,
        balance: CryptoValue
    ): TxValidationFailure {
        if (amount < minAvailable) {
            return TxValidationFailure(ValidationState.UNDER_MIN_LIMIT)
        }
        if (amount > balance) {
            return TxValidationFailure(ValidationState.INSUFFICIENT_FUNDS)
        }
        if (amount > maxAvailable)
            return TxValidationFailure(ValidationState.OVER_MAX_LIMIT)

        return TxValidationFailure(ValidationState.INVALID_AMOUNT)
    }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        walletManager.cancelAllPendingOrders().thenSingle {
            walletManager.createSwapOrder(
                direction = direction,
                quoteId = quotesEngine.getLatestQuote().transferQuote.id,
                volume = pendingTx.amount
            ).map {
                TxResult.UnHashedTxResult(pendingTx.amount) as TxResult
            }
        }
}