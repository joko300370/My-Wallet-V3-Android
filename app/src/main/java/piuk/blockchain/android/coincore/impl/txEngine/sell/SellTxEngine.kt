package piuk.blockchain.android.coincore.impl.txEngine.sell

import com.blockchain.swap.nabu.datamanagers.CustodialOrder
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.TransferLimits
import com.blockchain.swap.nabu.datamanagers.repositories.QuotesProvider
import com.blockchain.swap.nabu.models.nabu.KycTiers
import com.blockchain.swap.nabu.service.TierService
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.impl.txEngine.PricedQuote
import piuk.blockchain.android.coincore.impl.txEngine.QuotedEngine
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import java.math.RoundingMode

abstract class SellTxEngine(
    private val walletManager: CustodialWalletManager,
    kycTierService: TierService,
    quotesProvider: QuotesProvider,
    environmentConfig: EnvironmentConfig
) : QuotedEngine(quotesProvider, kycTierService, walletManager, environmentConfig) {

    val target: FiatAccount
        get() = txTarget as FiatAccount

    override val userFiat: String by lazy {
        target.fiatCurrency
    }

    override fun onLimitsForTierFetched(
        tier: KycTiers,
        limits: TransferLimits,
        pendingTx: PendingTx,
        pricedQuote: PricedQuote
    ): PendingTx {
        val exchangeRate = ExchangeRate.CryptoToFiat(
            sourceAccount.asset,
            userFiat,
            exchangeRates.getLastPrice(sourceAccount.asset, userFiat)
        )

        return pendingTx.copy(
            minLimit = (exchangeRate.inverse().convert(limits.minLimit) as CryptoValue).withUserDpRounding(
                RoundingMode.CEILING),
            maxLimit = (exchangeRate.inverse().convert(limits.maxLimit) as CryptoValue).withUserDpRounding(
                RoundingMode.FLOOR)
        )
    }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        validateAmount(pendingTx).updateTxValidity(pendingTx)

    private fun validateAmount(pendingTx: PendingTx): Completable {
        return sourceAccount.actionableBalance.flatMapCompletable { balance ->
            if (pendingTx.amount <= balance) {
                if (pendingTx.maxLimit != null && pendingTx.minLimit != null) {
                    when {
                        pendingTx.amount < pendingTx.minLimit -> throw TxValidationFailure(
                            ValidationState.UNDER_MIN_LIMIT)
                        pendingTx.amount > pendingTx.maxLimit -> throw TxValidationFailure(
                            ValidationState.OVER_MAX_LIMIT)
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

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> {
        return quotesEngine.pricedQuote.firstOrError().map { pricedQuote ->
            val latestQuoteExchangeRate = ExchangeRate.CryptoToFiat(
                from = sourceAccount.asset,
                to = userFiat,
                _rate = pricedQuote.price.toBigDecimal()
            )
            pendingTx.copy(
                confirmations = listOf(
                    TxConfirmationValue.ExchangePriceConfirmation(pricedQuote.price,
                        sourceAccount.asset),
                    TxConfirmationValue.From(sourceAccount.label),
                    TxConfirmationValue.To(txTarget.label),
                    TxConfirmationValue.NetworkFee(
                        fee = pendingTx.fees,
                        type = TxConfirmationValue.NetworkFee.FeeType.DEPOSIT_FEE,
                        asset = sourceAccount.asset
                    ),
                    TxConfirmationValue.Total(total = pendingTx.amount,
                        exchange = latestQuoteExchangeRate.convert(pendingTx.amount)
                    )
                ))
        }
    }

    override fun doRefreshConfirmations(pendingTx: PendingTx): Single<PendingTx> {
        return quotesEngine.pricedQuote.firstOrError().map { pricedQuote ->
            val latestQuoteExchangeRate = ExchangeRate.CryptoToFiat(
                from = sourceAccount.asset,
                to = userFiat,
                _rate = pricedQuote.price.toBigDecimal()
            )
            pendingTx.apply {
                addOrReplaceOption(
                    TxConfirmationValue.ExchangePriceConfirmation(
                        pricedQuote.price,
                        sourceAccount.asset
                    )
                )
                addOrReplaceOption(
                    TxConfirmationValue.Total(total = pendingTx.amount,
                        exchange = latestQuoteExchangeRate.convert(pendingTx.amount)
                    )
                )
            }
        }
    }

    protected fun createSellOrder(pendingTx: PendingTx): Single<CustodialOrder> =
        walletManager.createCustodialOrder(
            direction = direction,
            quoteId = quotesEngine.getLatestQuote().transferQuote.id,
            volume = pendingTx.amount
        ).doFinally {
            disposeQuotesFetching(pendingTx)
        }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAmount(pendingTx).updateTxValidity(pendingTx)

    override fun userExchangeRate(): Observable<ExchangeRate> = quotesEngine.pricedQuote.map {
        ExchangeRate.CryptoToFiat(
            from = sourceAccount.asset,
            to = userFiat,
            _rate = it.price.toBigDecimal()
        )
    }
}