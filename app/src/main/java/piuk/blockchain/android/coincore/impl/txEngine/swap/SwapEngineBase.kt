package piuk.blockchain.android.coincore.impl.txEngine.swap

import com.blockchain.swap.nabu.datamanagers.CurrencyPair
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.SwapDirection
import com.blockchain.swap.nabu.datamanagers.SwapOrder
import com.blockchain.swap.nabu.datamanagers.repositories.QuotesProvider
import com.blockchain.swap.nabu.models.nabu.KycTierLevel
import com.blockchain.swap.nabu.models.nabu.KycTiers
import com.blockchain.swap.nabu.models.nabu.NabuApiException
import com.blockchain.swap.nabu.models.nabu.NabuErrorCodes
import com.blockchain.swap.nabu.service.TierService
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Singles
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.copyAndPut
import piuk.blockchain.android.coincore.impl.txEngine.PricedQuote
import piuk.blockchain.android.coincore.impl.txEngine.SwapQuotesEngine
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import java.math.BigDecimal
import java.math.RoundingMode

private const val USER_TIER = "USER_TIER"

private val PendingTx.userTier: KycTiers
    get() = (this.engineState[USER_TIER] as KycTiers)

const val QUOTE_SUB = "quote_sub"
private val PendingTx.quoteSub: Disposable?
    get() = (this.engineState[QUOTE_SUB] as? Disposable)

abstract class SwapEngineBase(
    private val quotesProvider: QuotesProvider,
    private val walletManager: CustodialWalletManager,
    private val kycTierService: TierService
) : TxEngine() {

    protected abstract val direction: SwapDirection

    protected lateinit var quotesEngine: SwapQuotesEngine

    override fun start(
        sourceAccount: CryptoAccount,
        txTarget: TransactionTarget,
        exchangeRates: ExchangeRateDataManager,
        refreshTrigger: RefreshTrigger
    ) {
        super.start(sourceAccount, txTarget, exchangeRates, refreshTrigger)
        quotesEngine = SwapQuotesEngine(quotesProvider, direction, pair)
    }

    val target: CryptoAccount
        get() = txTarget as CryptoAccount

    protected fun updateLimits(pendingTx: PendingTx, pricedQuote: PricedQuote): Single<PendingTx> =
        Singles.zip(
            kycTierService.tiers(),
            walletManager.getSwapLimits(userFiat)
        ) { tier, limits ->

            val exchangeRate = ExchangeRate.CryptoToFiat(
                sourceAccount.asset,
                userFiat,
                exchangeRates.getLastPrice(sourceAccount.asset, userFiat).toBigDecimal()
            )

            pendingTx.copy(
                minLimit = (Money.max(
                    exchangeRate.inverse().convert(limits.minLimit),
                    minAmountToPayNetworkFees(pricedQuote.price,
                        pricedQuote.swapQuote.networkFee,
                        pricedQuote.swapQuote.staticFee
                    )) as CryptoValue).withUserDpRounding(RoundingMode.CEILING),
                maxLimit = (exchangeRate.inverse().convert(limits.maxLimit) as CryptoValue).withUserDpRounding(
                    RoundingMode.FLOOR),
                engineState = pendingTx.engineState.copyAndPut(USER_TIER, tier)
            )
        }

    protected fun Single<PendingTx>.handlePendingOrdersError(pendingTx: PendingTx): Single<PendingTx> =
        this.onErrorResumeNext {
            if (it is NabuApiException && it.getErrorCode() == NabuErrorCodes.PendingOrdersLimitReached) {
                Single.just(
                    pendingTx.copy(
                        validationState = ValidationState.PENDING_ORDERS_LIMIT_REACHED
                    )
                )
            } else Single.error(it)
        }

    override fun targetExchangeRate(): Observable<ExchangeRate> =
        quotesEngine.pricedQuote.map {
            ExchangeRate.CryptoToCrypto(
                from = sourceAccount.asset,
                to = target.asset,
                rate = it.price.toBigDecimal()
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

    private fun CryptoValue.withUserDpRounding(roundingMode: RoundingMode): CryptoValue =
        CryptoValue.fromMajor(this.currency, this.toBigDecimal().setScale(pair.source.userDp, roundingMode))

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAmount(pendingTx).updateTxValidity(pendingTx)

    protected fun Single<PendingTx>.updateQuotePrice(): Single<PendingTx> =
        doOnSuccess {
            quotesEngine.updateAmount(it.amount)
        }

    private val pair: CurrencyPair.CryptoCurrencyPair
        get() = CurrencyPair.CryptoCurrencyPair(sourceAccount.asset, target.asset)

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> {
        return quotesEngine.pricedQuote.firstOrError().flatMap { pricedQuote ->
            Single.just(
                pendingTx.copy(
                    confirmations = listOf(
                        TxConfirmationValue.SwapSourceValue(swappingAssetValue = pendingTx.amount as CryptoValue),
                        TxConfirmationValue.SwapReceiveValue(receiveAmount = CryptoValue.fromMajor(target.asset,
                            pendingTx.amount.toBigDecimal().times(pricedQuote.price.toBigDecimal()))),
                        TxConfirmationValue.SwapExchangeRate(CryptoValue.fromMajor(sourceAccount.asset, BigDecimal.ONE),
                            CryptoValue.fromMajor(target.asset, pricedQuote.price.toBigDecimal())),
                        TxConfirmationValue.From(from = sourceAccount.label),
                        TxConfirmationValue.To(to = txTarget.label),
                        TxConfirmationValue.NetworkFee(
                            fee = pricedQuote.swapQuote.networkFee,
                            type = TxConfirmationValue.NetworkFee.FeeType.WITHDRAWAL_FEE,
                            asset = target.asset
                        )
                    ),
                    minLimit = minLimit(pendingTx, pricedQuote.price)
                )
            )
        }.flatMap {
            startQuotesFetchingIfNotStarted(it)
        }
    }

    private fun minLimit(pendingTx: PendingTx, price: Money): Money =
        (Money.max(
            pendingTx.minLimit ?: minAmountToPayNetworkFees(
                price,
                quotesEngine.getLatestQuote().swapQuote.networkFee,
                quotesEngine.getLatestQuote().swapQuote.staticFee
            ), minAmountToPayNetworkFees(
                price,
                quotesEngine.getLatestQuote().swapQuote.networkFee,
                quotesEngine.getLatestQuote().swapQuote.staticFee
            )) as CryptoValue).withUserDpRounding(RoundingMode.CEILING)

    override fun doRefreshConfirmations(pendingTx: PendingTx): Single<PendingTx> {
        return quotesEngine.pricedQuote.firstOrError().map { pricedQuote ->
            pendingTx.copy(
                minLimit = minLimit(pendingTx, pricedQuote.price)
            ).apply {
                addOrReplaceOption(
                    TxConfirmationValue.NetworkFee(
                        fee = quotesEngine.getLatestQuote().swapQuote.networkFee,
                        type = TxConfirmationValue.NetworkFee.FeeType.WITHDRAWAL_FEE,
                        asset = target.asset
                    )
                )
                addOrReplaceOption(
                    TxConfirmationValue.SwapExchangeRate(
                        CryptoValue.fromMajor(sourceAccount.asset, BigDecimal.ONE),
                        pricedQuote.price
                    )
                )
                addOrReplaceOption(
                    TxConfirmationValue.SwapReceiveValue(receiveAmount = CryptoValue.fromMajor(target.asset,
                        pendingTx.amount.toBigDecimal().times(pricedQuote.price.toBigDecimal())))
                )
            }
        }
    }

    private fun startQuotesFetchingIfNotStarted(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            if (pendingTx.quoteSub == null) {
                pendingTx.copy(
                    engineState = pendingTx.engineState.copyAndPut(
                        QUOTE_SUB, startQuotesFetching()
                    )
                )
            } else {
                pendingTx
            }
        )

    private fun startQuotesFetching(): Disposable =
        quotesEngine.pricedQuote.doOnNext {
            refreshConfirmations(true)
        }.emptySubscribe()

    protected fun createOrder(pendingTx: PendingTx): Single<SwapOrder> =
        target.receiveAddress.flatMap {
            walletManager.createSwapOrder(
                direction = direction,
                quoteId = quotesEngine.getLatestQuote().swapQuote.id,
                volume = pendingTx.amount,
                destinationAddress = if (direction.requiresDestinationAddress()) it.address else null
            )
        }.doOnTerminate {
            pendingTx.quoteSub?.dispose()
        }

    override fun stop(pendingTx: PendingTx) {
        pendingTx.quoteSub?.dispose()
    }

    private fun SwapDirection.requiresDestinationAddress() =
        this == SwapDirection.ON_CHAIN || this == SwapDirection.TO_USERKEY

    private fun minAmountToPayNetworkFees(price: Money, networkFee: Money, staticFee: Money): Money =
        CryptoValue.fromMajor(
            pair.source,
            (networkFee.toBigDecimal().divide(price.toBigDecimal(), pair.source.dp, RoundingMode.HALF_UP)).plus(
                staticFee.toBigDecimal())
        )
}