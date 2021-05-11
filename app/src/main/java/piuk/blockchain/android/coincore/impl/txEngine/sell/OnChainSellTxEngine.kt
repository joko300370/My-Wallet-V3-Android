package piuk.blockchain.android.coincore.impl.txEngine.sell

import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.TransferDirection
import com.blockchain.nabu.service.TierService
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.Single
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FeeSelection
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.android.coincore.impl.txEngine.OnChainTxEngineBase
import piuk.blockchain.android.coincore.impl.txEngine.TransferQuotesEngine
import piuk.blockchain.android.coincore.updateTxValidity

class OnChainSellTxEngine(
    private val engine: OnChainTxEngineBase,
    walletManager: CustodialWalletManager,
    kycTierService: TierService,
    quotesEngine: TransferQuotesEngine,
    internalFeatureFlagApi: InternalFeatureFlagApi
) : SellTxEngineBase(
    walletManager, kycTierService, quotesEngine, internalFeatureFlagApi
) {
    override val direction: TransferDirection
        get() = TransferDirection.FROM_USERKEY

    override val availableBalance: Single<Money>
        get() = sourceAccount.accountBalance

    override fun assertInputsValid() {
        check(sourceAccount is CryptoNonCustodialAccount)
        check(txTarget is FiatAccount)
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        quotesEngine.pricedQuote
            .firstOrError()
            .doOnSuccess { pricedQuote ->
                engine.startFromQuote(pricedQuote)
            }.flatMap { quote ->
                engine.doInitialiseTx()
                    .flatMap {
                        updateLimits(it, quote)
                    }
            }.map { px ->
                px.copy(
                    feeSelection = defaultFeeSelection(px),
                    selectedFiat = userFiat
                )
            }.handlePendingOrdersError(
                PendingTx(
                    amount = CryptoValue.zero(sourceAsset),
                    totalBalance = CryptoValue.zero(sourceAsset),
                    availableBalance = CryptoValue.zero(sourceAsset),
                    feeForFullAvailable = CryptoValue.zero(sourceAsset),
                    feeAmount = CryptoValue.zero(sourceAsset),
                    selectedFiat = userFiat,
                    feeSelection = FeeSelection()
                )
            )

    private fun defaultFeeSelection(pendingTx: PendingTx): FeeSelection =
        when {
            pendingTx.feeSelection.availableLevels.contains(FeeLevel.Priority) -> {
                pendingTx.feeSelection.copy(
                    selectedLevel = FeeLevel.Priority,
                    availableLevels = setOf(FeeLevel.Priority)
                )
            }
            pendingTx.feeSelection.availableLevels.contains(FeeLevel.Regular) -> {
                pendingTx.feeSelection.copy(
                    selectedLevel = FeeLevel.Regular,
                    availableLevels = setOf(FeeLevel.Regular)
                )
            }
            else -> throw Exception("Not supported")
        }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        engine.doValidateAmount(pendingTx)
            .flatMap {
                if (
                    it.validationState == ValidationState.CAN_EXECUTE ||
                    it.validationState == ValidationState.INVALID_AMOUNT
                ) {
                    super.doValidateAmount(pendingTx)
                } else {
                    Single.just(it)
                }
            }.updateTxValidity(pendingTx)

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        engine.doValidateAll(pendingTx)
            .flatMap {
                if (
                    it.validationState == ValidationState.CAN_EXECUTE ||
                    it.validationState == ValidationState.INVALID_AMOUNT
                ) {
                    super.doValidateAll(pendingTx)
                } else {
                    Single.just(it)
                }
            }.updateTxValidity(pendingTx)

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> =
        engine.doUpdateAmount(amount, pendingTx)
            .updateQuotePrice()
            .clearConfirmations()

    override fun doUpdateFeeLevel(
        pendingTx: PendingTx,
        level: FeeLevel,
        customFeeAmount: Long
    ): Single<PendingTx> = Single.just(pendingTx)

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        createSellOrder(pendingTx)
            .flatMap { order ->
                engine.restartFromOrder(order, pendingTx)
                    .flatMap { px ->
                        engine.doExecute(px, secondPassword).updateOrderStatus(order.id)
                    }
            }
}