package piuk.blockchain.android.coincore.impl.txEngine.sell

import com.blockchain.nabu.datamanagers.CustodialOrder
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.TransferDirection
import com.blockchain.nabu.datamanagers.TransferLimits
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.service.TierService
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.NullAddress
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.impl.txEngine.PricedQuote
import piuk.blockchain.android.coincore.impl.txEngine.QuotedEngine
import piuk.blockchain.android.coincore.impl.txEngine.TransferQuotesEngine
import piuk.blockchain.android.coincore.updateTxValidity
import java.math.RoundingMode

abstract class SellTxEngineBase(
    private val walletManager: CustodialWalletManager,
    kycTierService: TierService,
    quotesEngine: TransferQuotesEngine
) : QuotedEngine(quotesEngine, kycTierService, walletManager, Product.SELL) {

    val target: FiatAccount
        get() = txTarget as FiatAccount

    override val userFiat: String
        get() = target.fiatCurrency

    override fun onLimitsForTierFetched(
        tier: KycTiers,
        limits: TransferLimits,
        pendingTx: PendingTx,
        pricedQuote: PricedQuote
    ): PendingTx {
        val exchangeRate = ExchangeRate.CryptoToFiat(
            sourceAsset,
            userFiat,
            exchangeRates.getLastPrice(sourceAsset, userFiat)
        )

        return pendingTx.copy(
            minLimit = (exchangeRate.inverse().convert(limits.minLimit) as CryptoValue)
                .withUserDpRounding(RoundingMode.CEILING),
            maxLimit = (exchangeRate.inverse().convert(limits.maxLimit) as CryptoValue)
                .withUserDpRounding(RoundingMode.FLOOR)
        )
    }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        validateAmount(pendingTx).updateTxValidity(pendingTx)

    private fun validateAmount(pendingTx: PendingTx): Completable {
        return availableBalance.flatMapCompletable { balance ->
            if (pendingTx.amount <= balance) {
                if (pendingTx.maxLimit != null && pendingTx.minLimit != null) {
                    when {
                        pendingTx.amount + pendingTx.feeAmount < pendingTx.minLimit -> throw TxValidationFailure(
                            ValidationState.UNDER_MIN_LIMIT
                        )
                        pendingTx.amount + pendingTx.feeAmount > pendingTx.maxLimit -> throw TxValidationFailure(
                            ValidationState.OVER_MAX_LIMIT
                        )
                        else -> Completable.complete()
                    }
                } else {
                    throw TxValidationFailure(ValidationState.UNKNOWN_ERROR)
                }
            } else {
                throw TxValidationFailure(ValidationState.INSUFFICIENT_FUNDS)
            }
        }
    }

    private fun buildNewFee(feeAmount: Money, exchangeAmount: Money): TxConfirmationValue? {
        return if (!feeAmount.isZero) {
            TxConfirmationValue.NetworkFee(
                feeAmount = feeAmount as CryptoValue,
                exchange = exchangeAmount,
                asset = sourceAsset.disambiguateERC20()
            )
        } else null
    }

    private fun CryptoCurrency.disambiguateERC20() = if (this.hasFeature(CryptoCurrency.IS_ERC20)) {
        CryptoCurrency.ETHER
    } else this

    private fun buildConfirmation(
        pendingTx: PendingTx,
        latestQuoteExchangeRate: ExchangeRate,
        pricedQuote: PricedQuote
    ): PendingTx =
        pendingTx.copy(
            confirmations = listOfNotNull(
                TxConfirmationValue.ExchangePriceConfirmation(pricedQuote.price, sourceAsset),
                TxConfirmationValue.To(
                    txTarget,
                    AssetAction.Sell
                ),
                TxConfirmationValue.Sale(
                    amount = pendingTx.amount,
                    exchange = latestQuoteExchangeRate.convert(pendingTx.amount)
                ),
                buildNewFee(pendingTx.feeAmount, latestQuoteExchangeRate.convert(pendingTx.feeAmount)),
                TxConfirmationValue.Total(
                    totalWithFee = (pendingTx.amount as CryptoValue).plus(
                        pendingTx.feeAmount as CryptoValue
                    ),
                    exchange = latestQuoteExchangeRate.convert(
                        pendingTx.amount.plus(pendingTx.feeAmount)
                    )
                )
            )
        )

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        quotesEngine.pricedQuote
            .firstOrError()
            .map { pricedQuote ->
                val latestQuoteExchangeRate = ExchangeRate.CryptoToFiat(
                    from = sourceAsset,
                    to = userFiat,
                    _rate = pricedQuote.price.toBigDecimal()
                )
                buildConfirmation(pendingTx, latestQuoteExchangeRate, pricedQuote)
            }

    private fun addOrRefreshConfirmations(
        pendingTx: PendingTx,
        pricedQuote: PricedQuote,
        latestQuoteExchangeRate: ExchangeRate
    ): PendingTx =
        pendingTx.apply {
            addOrReplaceOption(
                TxConfirmationValue.ExchangePriceConfirmation(pricedQuote.price, sourceAsset)
            )
            addOrReplaceOption(
                TxConfirmationValue.Total(
                    totalWithFee = (pendingTx.amount as CryptoValue).plus(
                        pendingTx.feeAmount as CryptoValue
                    ),
                    exchange = latestQuoteExchangeRate.convert(
                        pendingTx.amount.plus(pendingTx.feeAmount)
                    )
                )
            )
        }

    override fun doRefreshConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        quotesEngine.pricedQuote
            .firstOrError()
            .map { pricedQuote ->
                val latestQuoteExchangeRate = ExchangeRate.CryptoToFiat(
                    from = sourceAsset,
                    to = userFiat,
                    _rate = pricedQuote.price.toBigDecimal()
                )
                    addOrRefreshConfirmations(pendingTx, pricedQuote, latestQuoteExchangeRate)
            }

    protected fun createSellOrder(pendingTx: PendingTx): Single<CustodialOrder> =
        sourceAccount.receiveAddress
            .onErrorReturn { NullAddress }
            .flatMap { refAddress ->
                walletManager.createCustodialOrder(
                    direction = direction,
                    quoteId = quotesEngine.getLatestQuote().transferQuote.id,
                    volume = pendingTx.amount,
                    refundAddress = if (direction.requiresRefundAddress()) refAddress.address else null
                ).doFinally {
                    disposeQuotesFetching(pendingTx)
                }
            }

    private fun TransferDirection.requiresRefundAddress() =
        this == TransferDirection.FROM_USERKEY

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAmount(pendingTx).updateTxValidity(pendingTx)

    override fun userExchangeRate(): Observable<ExchangeRate> =
        quotesEngine.pricedQuote.map {
            ExchangeRate.CryptoToFiat(
                from = sourceAsset,
                to = userFiat,
                _rate = it.price.toBigDecimal()
            )
        }
}