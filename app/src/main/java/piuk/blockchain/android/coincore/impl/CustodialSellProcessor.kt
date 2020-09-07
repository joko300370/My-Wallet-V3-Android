package piuk.blockchain.android.coincore.impl

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.OrderInput
import com.blockchain.swap.nabu.datamanagers.OrderOutput
import com.blockchain.swap.nabu.datamanagers.Quote
import com.blockchain.swap.nabu.models.simplebuy.CustodialWalletOrder
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionProcessor
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.toCrypto
import piuk.blockchain.androidcore.utils.extensions.then
import java.lang.IllegalStateException
import java.math.RoundingMode

class CustodialSellProcessor(
    sendingAccount: CryptoAccount,
    sendTarget: FiatAccount,
    private val walletManager: CustodialWalletManager,
    exchangeRates: ExchangeRateDataManager
) : TransactionProcessor(sendingAccount, sendTarget, exchangeRates) {

    private lateinit var order: CustodialWalletOrder

    private val fiatCurrency: String
        get() = (sendTarget as? FiatAccount)?.fiatCurrency
            ?: throw IllegalStateException("send target should be fiat account")

    private val cryptoCurrency: CryptoCurrency
        get() = sendingAccount.asset

    override fun doInitialiseTx(): Single<PendingTx> =
        walletManager.getSupportedBuySellCryptoCurrencies((sendTarget as FiatAccount).fiatCurrency).map {
            it.pairs.first {
                it.cryptoCurrency == sendingAccount.asset && it.fiatCurrency == sendTarget.fiatCurrency
            }
        }.flatMap { pair ->
            Single.just(
                PendingTx(
                    amount = FiatValue.zero(sendTarget.fiatCurrency),
                    available = CryptoValue.zero(sendingAccount.asset),
                    fees = CryptoValue.zero(sendingAccount.asset),
                    selectedFiat = sendTarget.fiatCurrency,
                    maxLimit = pair.sellLimits.maxLimit(sendTarget.fiatCurrency),
                    minLimit = pair.sellLimits.minLimit(sendTarget.fiatCurrency),
                    feeLevel = FeeLevel.None
                ))
        }

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        return sendingAccount.actionableBalance
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
                    pair = "${sendingAccount.asset.networkTicker}-${(sendTarget as FiatAccount).fiatCurrency}",
                    action = "SELL",
                    input = OrderInput(
                        sendingAccount.asset.toString(),
                        tx.amount.takeIf { it is CryptoValue }?.toBigInteger().toString()
                    ),
                    output = OrderOutput(
                        sendTarget.fiatCurrency,
                        tx.amount.takeIf { it is FiatValue }?.toBigInteger().toString()
                    )
                )
            }
    }

    private fun maxLimit(amount: Money, pendingTx: PendingTx): Money? =
        when (amount) {
            is FiatValue -> pendingTx.maxLimit?.toFiat(exchangeRates, amount.currencyCode)
            is CryptoValue -> (pendingTx.maxLimit as? FiatValue)?.let {
                it.toCrypto(exchangeRates, amount.currency)
            } ?: pendingTx.maxLimit
            else -> throw IllegalStateException("Unknown money type")
        }

    private fun minLimit(amount: Money, pendingTx: PendingTx): Money? =
        when (amount) {
            is FiatValue -> pendingTx.minLimit?.toFiat(exchangeRates, amount.currencyCode)
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
            cryptoCurrency = sendingAccount.asset,
            fiatCurrency = fiatCurrency,
            amount = pendingTx.amount.toBigInteger().toString(),
            action = "SELL",
            currency = pendingTx.amount.currencyCode
        ).map {
            updateOptionFromQuote(it, pendingTx)
        }
    }

    private fun updateOptionFromQuote(quote: Quote, pendingTx: PendingTx): PendingTx {

        val options = listOf(
            TxOptionValue.ExchangePriceOption(quote.rate, sendingAccount.asset),
            TxOptionValue.From(sendingAccount.label),
            TxOptionValue.To(sendTarget.label),
            TxOptionValue.Total(if (pendingTx.amount is FiatValue) pendingTx.amount else
                FiatValue.fromMajor(fiatCurrency,
                    pendingTx.amount.toBigDecimal().times(quote.rate.toBigDecimal()))
            )
        )

        return pendingTx.copy(
            options = options,
            amount = if (pendingTx.amount is CryptoValue) pendingTx.amount else
                CryptoValue.fromMajor(
                    sendingAccount.asset,
                    (pendingTx.amount.toBigDecimal().divide(quote.rate.toBigDecimal(),
                        sendingAccount.asset.userDp, RoundingMode.HALF_UP
                    ))
                )
        )
    }

    private fun validateAmounts(pendingTx: PendingTx): Completable =
        sendingAccount.actionableBalance.map { it as CryptoValue }
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