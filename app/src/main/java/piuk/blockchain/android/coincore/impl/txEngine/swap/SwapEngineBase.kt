package piuk.blockchain.android.coincore.impl.txEngine.swap

import com.blockchain.swap.nabu.datamanagers.CurrencyPair
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.Direction
import com.blockchain.swap.nabu.datamanagers.SwapOrder
import com.blockchain.swap.nabu.datamanagers.repositories.QuotesProvider
import com.blockchain.swap.nabu.models.nabu.KycTierLevel
import com.blockchain.swap.nabu.models.nabu.KycTiers
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
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.copyAndPut
import piuk.blockchain.android.coincore.impl.txEngine.SwapQuotesEngine
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.toCrypto
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import java.math.BigDecimal

private const val USER_TIER = "USER_TIER"

private val PendingTx.userTier: KycTiers
    get() = (this.engineState[USER_TIER] as KycTiers)

const val QUOTE_SUB = "quote_sub"
private val PendingTx.quoteSub: Disposable?
    get() = (this.engineState[QUOTE_SUB] as? Disposable)

const val RATES_SUB = "rates_sub"
private val PendingTx.ratesSub: Disposable?
    get() = (this.engineState[RATES_SUB] as? Disposable)

abstract class SwapEngineBase(
    private val isNoteSupported: Boolean,
    private val quotesProvider: QuotesProvider,
    private val walletManager: CustodialWalletManager,
    private val kycTierService: TierService
) : TxEngine() {

    protected abstract val direction: Direction

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

    override fun doInitialiseTx(): Single<PendingTx> =
        Singles.zip(
            kycTierService.tiers(),
            walletManager.getSwapLimits(userFiat)
        ) { tier, limits ->
            PendingTx(
                amount = CryptoValue.zero(sourceAccount.asset),
                available = CryptoValue.zero(sourceAccount.asset),
                fees = CryptoValue.zero(sourceAccount.asset),
                feeLevel = FeeLevel.None,
                selectedFiat = userFiat,
                minLimit = limits.minLimit.toCrypto(exchangeRates, sourceAccount.asset),
                maxLimit = limits.maxLimit.toCrypto(exchangeRates, sourceAccount.asset),
                engineState = mapOf(
                    USER_TIER to tier
                )
            )
        }

    override fun targetExchangeRate(): Observable<ExchangeRate> =
        quotesEngine.rate.map {
            ExchangeRate.CryptoToCrypto(
                from = sourceAccount.asset,
                to = target.asset,
                rate = it
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

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAmount(pendingTx).updateTxValidity(pendingTx)

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> =
        sourceAccount.actionableBalance.map { balance -> balance as CryptoValue }.map { available ->
            pendingTx.copy(
                amount = amount,
                available = available
            )
        }.doOnSuccess {
            quotesEngine.updateAmount(it.amount.toBigDecimal())
        }

    private val pair: CurrencyPair.CryptoCurrencyPair
        get() = CurrencyPair.CryptoCurrencyPair(sourceAccount.asset, target.asset)

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> {
        return quotesEngine.rate.firstOrError().flatMap { rate ->
            Single.just(
                pendingTx.copy(
                    options = listOf(
                        TxOptionValue.SwapSourceValue(swappingAssetValue = pendingTx.amount as CryptoValue),
                        TxOptionValue.SwapReceiveValue(receiveAmount = CryptoValue.fromMajor(target.asset,
                            pendingTx.amount.toBigDecimal().times(rate))),
                        TxOptionValue.SwapExchangeRate(CryptoValue.fromMajor(sourceAccount.asset, BigDecimal.ONE),
                            CryptoValue.fromMajor(target.asset, rate)),
                        TxOptionValue.From(from = sourceAccount.label),
                        TxOptionValue.To(to = txTarget.label),
                        TxOptionValue.NetworkFee(
                            fee = quotesEngine.getLatestQuote().networkFee
                        )
                    )
                )
            )
        }.flatMap {
            startQuotesFetchingIfNotStarted(it)
        }.flatMap {
            startRatesFetchingIfNotStarted(it)
        }
    }

    private fun startRatesFetchingIfNotStarted(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            if (pendingTx.ratesSub == null) {
                pendingTx.copy(
                    engineState = pendingTx.engineState.copyAndPut(
                        RATES_SUB, startRatesFetching()
                    )
                )
            } else {
                pendingTx
            })

    private fun startRatesFetching(): Disposable =
        quotesEngine.rate.doOnNext {
            refreshConfirmations(false)
        }.emptySubscribe()

    override fun doRefreshConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        quotesEngine.rate.firstOrError().flatMap { rate ->
            Single.just(pendingTx.apply {
                addOrReplaceOption(
                    TxOptionValue.NetworkFee(
                        fee = quotesEngine.getLatestQuote().networkFee
                    )
                )
                addOrReplaceOption(
                    TxOptionValue.SwapExchangeRate(
                        CryptoValue.fromMajor(sourceAccount.asset, BigDecimal.ONE),
                        CryptoValue.fromMajor(target.asset, rate)
                    )
                )
                addOrReplaceOption(
                    TxOptionValue.SwapReceiveValue(receiveAmount = CryptoValue.fromMajor(target.asset,
                        pendingTx.amount.toBigDecimal().times(rate)))
                )
            })
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
        quotesEngine.quote.doOnNext {
            refreshConfirmations(false)
        }.emptySubscribe()

    protected fun createOrder(pendingTx: PendingTx): Single<SwapOrder> =
        target.receiveAddress.flatMap {
            walletManager.createSwapOrder(
                direction = direction,
                quoteId = quotesEngine.getLatestQuote().id,
                volume = pendingTx.amount,
                destinationAddress = if (direction.requiresDestinationAddress()) it.address else null
            )
        }.doOnTerminate {
            pendingTx.quoteSub?.dispose()
            pendingTx.ratesSub?.dispose()
        }

    override fun stop(pendingTx: PendingTx) {
        pendingTx.quoteSub?.dispose()
        pendingTx.ratesSub?.dispose()
    }

    private fun Direction.requiresDestinationAddress() =
        this == Direction.ON_CHAIN || this == Direction.TO_USERKEY
}
