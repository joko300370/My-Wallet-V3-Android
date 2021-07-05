package piuk.blockchain.android.coincore.impl.txEngine.swap

import androidx.annotation.VisibleForTesting
import com.blockchain.nabu.datamanagers.CustodialOrder
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.TransferDirection
import com.blockchain.nabu.datamanagers.TransferLimits
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.service.TierService
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.NullAddress
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.copyAndPut
import piuk.blockchain.android.coincore.impl.txEngine.PricedQuote
import piuk.blockchain.android.coincore.impl.txEngine.QuotedEngine
import piuk.blockchain.android.coincore.impl.txEngine.TransferQuotesEngine
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.android.ui.transactionflow.flow.FeeInfo
import java.math.BigDecimal
import java.math.RoundingMode

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
const val USER_TIER = "USER_TIER"

const val RECEIVE_AMOUNT = "RECEIVE_AMOUNT"
const val OUTGOING_FEE = "OUTGOING_FEE"

private val PendingTx.userTier: KycTiers
    get() = (this.engineState[USER_TIER] as KycTiers)

abstract class SwapTxEngineBase(
    quotesEngine: TransferQuotesEngine,
    private val walletManager: CustodialWalletManager,
    kycTierService: TierService
) : QuotedEngine(quotesEngine, kycTierService, walletManager, Product.TRADE) {

    private lateinit var minApiLimit: Money

    val target: CryptoAccount
        get() = txTarget as CryptoAccount

    override fun targetExchangeRate(): Observable<ExchangeRate> =
        quotesEngine.pricedQuote.map {
            ExchangeRate.CryptoToCrypto(
                from = sourceAsset,
                to = target.asset,
                rate = it.price.toBigDecimal()
            )
        }

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

        minApiLimit = exchangeRate.inverse()
            .convert(limits.minLimit) as CryptoValue

        return pendingTx.copy(
            minLimit = minLimit(pricedQuote.price),
            maxLimit = (exchangeRate.inverse().convert(limits.maxLimit) as CryptoValue)
                .withUserDpRounding(RoundingMode.FLOOR),
            engineState = pendingTx.engineState.copyAndPut(USER_TIER, tier)
        )
    }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        validateAmount(pendingTx).updateTxValidity(pendingTx)

    private fun validateAmount(pendingTx: PendingTx): Completable {
        return availableBalance.flatMapCompletable { balance ->
            if (pendingTx.amount <= balance) {
                if (pendingTx.maxLimit != null && pendingTx.minLimit != null) {
                    when {
                        pendingTx.amount < pendingTx.minLimit -> throw TxValidationFailure(
                            ValidationState.UNDER_MIN_LIMIT
                        )
                        pendingTx.amount > pendingTx.maxLimit -> throw validationFailureForTier(pendingTx)
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

    private fun validationFailureForTier(pendingTx: PendingTx) =
        if (pendingTx.userTier.isApprovedFor(KycTierLevel.GOLD)) {
            TxValidationFailure(ValidationState.OVER_GOLD_TIER_LIMIT)
        } else {
            TxValidationFailure(ValidationState.OVER_SILVER_TIER_LIMIT)
        }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAmount(pendingTx).updateTxValidity(pendingTx)

    private fun buildConfirmations(pendingTx: PendingTx, pricedQuote: PricedQuote): PendingTx =
        pendingTx.copy(
            confirmations = listOfNotNull(
                TxConfirmationValue.SwapExchange(
                    CryptoValue.fromMajor(sourceAsset, BigDecimal.ONE),
                    CryptoValue.fromMajor(target.asset, pricedQuote.price.toBigDecimal())
                ),
                TxConfirmationValue.CompoundNetworkFee(
                    if (pendingTx.feeAmount.isZero) {
                        null
                    } else
                        FeeInfo(
                            pendingTx.feeAmount,
                            pendingTx.feeAmount.toFiat(exchangeRates, userFiat),
                            sourceAsset
                        ),
                    if (pricedQuote.transferQuote.networkFee.isZero) {
                        null
                    } else
                        FeeInfo(
                            pricedQuote.transferQuote.networkFee,
                            pricedQuote.transferQuote.networkFee.toFiat(exchangeRates, userFiat),
                            target.asset
                        )
                )
            )
        ).also {
            it.engineState.copyAndPut(RECEIVE_AMOUNT, pricedQuote.price.toBigDecimal())
            it.engineState.copyAndPut(OUTGOING_FEE, pricedQuote.transferQuote.networkFee)
        }

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        quotesEngine.pricedQuote
            .firstOrError()
            .map { pricedQuote ->
                buildConfirmations(pendingTx, pricedQuote)
            }

    private fun minLimit(price: Money): Money {
        val minAmountToPayFees = minAmountToPayNetworkFees(
            price,
            quotesEngine.getLatestQuote().transferQuote.networkFee
        )
        return minApiLimit.plus(minAmountToPayFees).withUserDpRounding(RoundingMode.CEILING)
    }

    private fun addOrReplaceConfirmations(pendingTx: PendingTx, pricedQuote: PricedQuote): PendingTx =
        pendingTx.copy(
            minLimit = minLimit(pricedQuote.price)
        ).apply {
            addOrReplaceOption(
                TxConfirmationValue.SwapExchange(
                    CryptoValue.fromMajor(sourceAsset, BigDecimal.ONE),
                    CryptoValue.fromMajor(target.asset, pricedQuote.price.toBigDecimal())
                )
            )
            addOrReplaceOption(
                TxConfirmationValue.CompoundNetworkFee(
                    if (pendingTx.feeAmount.isZero) {
                        null
                    } else
                        FeeInfo(
                            pendingTx.feeAmount,
                            pendingTx.feeAmount.toFiat(exchangeRates, userFiat),
                            sourceAsset
                        ),
                    if (pricedQuote.transferQuote.networkFee.isZero) {
                        null
                    } else
                        FeeInfo(
                            pricedQuote.transferQuote.networkFee,
                            pricedQuote.transferQuote.networkFee.toFiat(exchangeRates, userFiat),
                            target.asset
                        )
                )
            )
        }

    override fun doRefreshConfirmations(pendingTx: PendingTx): Single<PendingTx> {
        return quotesEngine.pricedQuote.firstOrError().map { pricedQuote ->
            addOrReplaceConfirmations(pendingTx, pricedQuote)
        }
    }

    protected fun createOrder(pendingTx: PendingTx): Single<CustodialOrder> =
        target.receiveAddress.zipWith(sourceAccount.receiveAddress.onErrorReturn { NullAddress })
            .flatMap { (destinationAddr, refAddress) ->
                walletManager.createCustodialOrder(
                    direction = direction,
                    quoteId = quotesEngine.getLatestQuote().transferQuote.id,
                    volume = pendingTx.amount,
                    destinationAddress = if (direction.requiresDestinationAddress()) destinationAddr.address else null,
                    refundAddress = if (direction.requireRefundAddress()) refAddress.address else null
                )
            }.doFinally {
                disposeQuotesFetching(pendingTx)
            }

    private fun TransferDirection.requiresDestinationAddress() =
        this == TransferDirection.ON_CHAIN || this == TransferDirection.TO_USERKEY

    private fun TransferDirection.requireRefundAddress() =
        this == TransferDirection.ON_CHAIN || this == TransferDirection.FROM_USERKEY

    private fun minAmountToPayNetworkFees(price: Money, networkFee: Money): Money =
        CryptoValue.fromMajor(
            sourceAsset,
            networkFee.toBigDecimal()
                .divide(price.toBigDecimal(), sourceAsset.dp, RoundingMode.HALF_UP)
        )
}
