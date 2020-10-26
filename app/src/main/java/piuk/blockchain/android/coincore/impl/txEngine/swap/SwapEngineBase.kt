package piuk.blockchain.android.coincore.impl.txEngine.swap

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
import io.reactivex.rxkotlin.Singles
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.impl.txEngine.SwapQuotesEngine
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.toCrypto

private const val USER_TIER = "USER_TIER"

private val PendingTx.userTier: KycTiers
    get() = (this.engineState[USER_TIER] as KycTiers)

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
        quotesEngine.getRate().map {
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
                pendingTx.maxLimit?.let { limit ->
                    if (pendingTx.amount > limit) {
                        throw validationFailureForTier(pendingTx)
                    } else {
                        Completable.complete()
                    }
                } ?: throw TxValidationFailure(ValidationState.UNKNOWN_ERROR)
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

    private val pair: String
        get() = "${sourceAccount.asset.networkTicker}-${target.asset.networkTicker}"

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            pendingTx.copy(
                options = pendingTx.options.toMutableList().apply {
                    add(TxOptionValue.SwapSourceValue(swappingAssetValue = pendingTx.amount as CryptoValue))
                    /*   add(TxOptionValue.SwapDestinationValue(
                           receivingAssetValue = targetRate.value?.convert(pendingTx.amount) as CryptoValue))*/
                    add(TxOptionValue.From(from = sourceAccount.label))
                    add(TxOptionValue.To(to = txTarget.label))
                    add(TxOptionValue.FeedTotal(
                        amount = pendingTx.amount,
                        fee = pendingTx.fees,
                        exchangeFee = pendingTx.fees.toFiat(exchangeRates, userFiat),
                        exchangeAmount = pendingTx.amount.toFiat(exchangeRates, userFiat)
                    ))

                    if (isNoteSupported) {
                        add(TxOptionValue.Description())
                    }
                }.toList()
            )
        )

    protected fun createOrder(pendingTx: PendingTx): Single<SwapOrder> =
        target.receiveAddress.flatMap {
            walletManager.createSwapOrder(
                direction = direction,
                quoteId = quotesEngine.getLatestQuote().id,
                volume = pendingTx.amount,
                destinationAddress = if (direction.requiresDestinationAddress()) it.address else null
            )
        }

    private fun Direction.requiresDestinationAddress() = this == Direction.ON_CHAIN || this == Direction.TO_USERKEY
}
