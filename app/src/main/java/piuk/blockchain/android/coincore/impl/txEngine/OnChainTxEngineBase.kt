package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.preferences.WalletStatus
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FeeState
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.TxResult

abstract class OnChainTxEngineBase(
    override val requireSecondPassword: Boolean,
    private val walletPreferences: WalletStatus
) : TxEngine() {

    override fun assertInputsValid() {
        val tgt = txTarget
        check(tgt is CryptoAddress)
        check(tgt.address.isNotEmpty())
        check(sourceAsset == tgt.asset)
    }

    override fun doPostExecute(pendingTx: PendingTx, txResult: TxResult): Completable =
        txTarget.onTxCompleted(txResult)

    protected fun mapSavedFeeToFeeLevel(feeType: Int?): FeeLevel =
        when (feeType) {
            FeeLevel.Priority.ordinal -> FeeLevel.Priority
            FeeLevel.Regular.ordinal -> FeeLevel.Regular
            else -> FeeLevel.Regular
        }

    private fun FeeLevel.mapFeeLevelToSavedValue() =
        this.ordinal

    private fun storeDefaultFeeLevel(cryptoCurrency: CryptoCurrency, feeLevel: FeeLevel) =
        walletPreferences.setFeeTypeForAsset(cryptoCurrency, feeLevel.mapFeeLevelToSavedValue())

    protected fun fetchDefaultFeeLevel(cryptoCurrency: CryptoCurrency): Int? =
        walletPreferences.getFeeTypeForAsset(cryptoCurrency)

    protected fun getFeeState(pTx: PendingTx, feeOptions: FeeOptions? = null) =
        if (pTx.feeSelection.selectedLevel == FeeLevel.Custom) {
            when {
                pTx.feeSelection.customAmount == -1L -> FeeState.ValidCustomFee
                pTx.feeSelection.customAmount < MINIMUM_CUSTOM_FEE -> {
                    FeeState.FeeUnderMinLimit
                }
                pTx.feeSelection.customAmount >= MINIMUM_CUSTOM_FEE &&
                    pTx.feeSelection.customAmount <= feeOptions?.limits?.min ?: 0L -> {
                    FeeState.FeeUnderRecommended
                }
                pTx.feeSelection.customAmount >= feeOptions?.limits?.max ?: 0L -> {
                    FeeState.FeeOverRecommended
                }
                else -> FeeState.ValidCustomFee
            }
        } else {
            if (pTx.availableBalance < pTx.amount) {
                FeeState.FeeTooHigh
            } else {
                FeeState.FeeDetails(pTx.feeAmount)
            }
        }

    final override fun doUpdateFeeLevel(
        pendingTx: PendingTx,
        level: FeeLevel,
        customFeeAmount: Long
    ): Single<PendingTx> {
        require(pendingTx.feeSelection.availableLevels.contains(level))

        return if (pendingTx.hasFeeLevelChanged(level, customFeeAmount)) {
            updateFeeSelection(
                sourceAsset,
                pendingTx,
                level,
                customFeeAmount
            )
        } else {
            Single.just(pendingTx)
        }
    }

    private fun updateFeeSelection(
        cryptoCurrency: CryptoCurrency,
        pendingTx: PendingTx,
        newFeeLevel: FeeLevel,
        customFeeAmount: Long
    ): Single<PendingTx> {
        storeDefaultFeeLevel(cryptoCurrency, newFeeLevel)

        return doUpdateAmount(
            amount = pendingTx.amount,
            pendingTx = pendingTx.copy(
                feeSelection = pendingTx.feeSelection.copy(
                    selectedLevel = newFeeLevel,
                    customAmount = customFeeAmount
                )
            )
        )
    }

    private fun PendingTx.hasFeeLevelChanged(newLevel: FeeLevel, newAmount: Long) =
        with(feeSelection) {
            selectedLevel != newLevel || (selectedLevel == FeeLevel.Custom && newAmount != customAmount)
        }

    companion object {
        const val MINIMUM_CUSTOM_FEE = 1L
    }
}
