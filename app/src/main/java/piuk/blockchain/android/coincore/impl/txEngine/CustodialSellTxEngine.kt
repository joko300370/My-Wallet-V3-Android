package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.OrderInput
import com.blockchain.swap.nabu.datamanagers.OrderOutput
import com.blockchain.swap.nabu.datamanagers.Quote
import com.blockchain.swap.nabu.models.simplebuy.CustodialWalletOrder
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.androidcore.data.exchangerate.toCrypto
import piuk.blockchain.androidcore.utils.extensions.then
import piuk.blockchain.androidcore.utils.extensions.thenSingle
import java.lang.IllegalStateException
import java.math.RoundingMode

class CustodialSellTxEngine(
    private val walletManager: CustodialWalletManager
) : TxEngine() {

    private lateinit var order: CustodialWalletOrder

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

    private val cryptoCurrency: CryptoCurrency
        get() = sourceAccount.asset

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
                        Single.just(
                            PendingTx(
                                amount = FiatValue.zero(fiatTarget.fiatCurrency),
                                available = CryptoValue.zero(sourceAccount.asset),
                                fees = CryptoValue.zero(sourceAccount.asset),
                                selectedFiat = userFiat,
                                maxLimit = pair.sellLimits.maxLimit(fiatTarget.fiatCurrency),
                                minLimit = pair.sellLimits.minLimit(fiatTarget.fiatCurrency),
                                feeLevel = FeeLevel.None
                            )
                        )
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
            }.doOnSuccess { tx ->
                order = CustodialWalletOrder(
                    pair = "${sourceAccount.asset.networkTicker}-${fiatTarget.fiatCurrency}",
                    action = "SELL",
                    input = OrderInput(
                        sourceAccount.asset.networkTicker,
                        tx.amount.takeIf { it is CryptoValue }?.toBigInteger().toString()
                    ),
                    output = OrderOutput(
                        fiatTarget.fiatCurrency,
                        tx.amount.takeIf { it is FiatValue }?.toBigInteger().toString()
                    )
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

    override val canTransactFiat: Boolean
        get() = true

    override val requireSecondPassword: Boolean
        get() = false

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx).updateTxValidity(pendingTx)

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx).updateTxValidity(pendingTx)

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> {
        return walletManager.getQuote(
            cryptoCurrency = sourceAccount.asset,
            fiatCurrency = userFiat,
            amount = pendingTx.amount.toBigInteger().toString(),
            action = "SELL",
            currency = pendingTx.amount.currencyCode
        ).map {
            updateOptionFromQuote(it, pendingTx)
        }
    }

    private fun updateOptionFromQuote(quote: Quote, pendingTx: PendingTx): PendingTx {

        val options = listOf(
            TxOptionValue.ExchangePriceOption(quote.rate, sourceAccount.asset),
            TxOptionValue.From(sourceAccount.label),
            TxOptionValue.To(fiatTarget.label),
            TxOptionValue.Total(if (pendingTx.amount is FiatValue) pendingTx.amount else
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
            options = options,
            amount = if (pendingTx.amount is CryptoValue) pendingTx.amount else
                exchangeRate.convert(pendingTx.amount),
            minLimit = if (pendingTx.minLimit is CryptoValue) pendingTx.minLimit else
                exchangeRate.convert(pendingTx.minLimit ?: FiatValue.zero(fiatTarget.fiatCurrency)),
            maxLimit = if (pendingTx.maxLimit is CryptoValue) pendingTx.maxLimit else
                exchangeRate.convert(pendingTx.maxLimit ?: FiatValue.zero(fiatTarget.fiatCurrency))
        )
    }

    private fun validateAmounts(pendingTx: PendingTx): Completable =
        sourceAccount.accountBalance.map { it as CryptoValue }
            .flatMapCompletable { balance ->
                val cryptoAmount = (pendingTx.amount as? FiatValue)?.toCrypto(exchangeRates, cryptoCurrency)
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

    override val feeOptions: Set<FeeLevel>
        get() = setOf(FeeLevel.None)

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Completable =
        walletManager.cancelAllPendingOrders().then {
            walletManager.createOrder(
                custodialWalletOrder = order,
                stateAction = "pending"
            ).flatMap {
                walletManager.confirmOrder(it.id, null)
            }.ignoreElement()
        }
}