package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.preferences.WalletStatus
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeState
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxConfirmationValue
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
        check(sourceAccount.asset == tgt.asset)
    }

    override fun doPostExecute(txResult: TxResult): Completable =
        txTarget.onTxCompleted(txResult)

    protected fun mapSavedFeeToFeeLevel(feeType: Int?): FeeLevel =
        when (feeType) {
            FeeLevel.Priority.ordinal -> FeeLevel.Priority
            FeeLevel.Regular.ordinal -> FeeLevel.Regular
            else -> FeeLevel.Regular
        }

    private fun FeeLevel.mapFeeLevelToSavedValue() =
        this.ordinal

    private fun setFeeType(cryptoCurrency: CryptoCurrency, feeLevel: FeeLevel) =
        walletPreferences.setFeeTypeForAsset(cryptoCurrency, feeLevel.mapFeeLevelToSavedValue())

    protected fun getFeeType(cryptoCurrency: CryptoCurrency): Int? =
        walletPreferences.getFeeTypeForAsset(cryptoCurrency)

    protected fun getFeeState(pTx: PendingTx, feeOptions: FeeOptions? = null) =
        if (pTx.feeLevel == FeeLevel.Custom) {
            when {
                pTx.customFeeAmount == -1L -> FeeState.ValidCustomFee
                pTx.customFeeAmount < MINIMUM_CUSTOM_FEE -> {
                    FeeState.FeeUnderMinLimit
                }
                pTx.customFeeAmount >= MINIMUM_CUSTOM_FEE &&
                    pTx.customFeeAmount <= feeOptions?.limits?.min ?: 0L -> {
                    FeeState.FeeUnderRecommended
                }
                pTx.customFeeAmount >= feeOptions?.limits?.max ?: 0L -> {
                    FeeState.FeeOverRecommended
                }
                else -> FeeState.ValidCustomFee
            }
        } else {
            if (pTx.availableBalance < pTx.amount) {
                FeeState.FeeTooHigh
            } else {
                FeeState.FeeDetails(pTx.fees)
            }
        }

    protected fun updateFeeSelection(
        cryptoCurrency: CryptoCurrency,
        pendingTx: PendingTx,
        newConfirmation: TxConfirmationValue.FeeSelection
    ): Single<PendingTx> {
        setFeeType(cryptoCurrency, newConfirmation.selectedLevel)

        return doUpdateAmount(
            pendingTx.amount, pendingTx.copy(
                feeLevel = newConfirmation.selectedLevel,
                customFeeAmount = newConfirmation.customFeeAmount
            )
        )
            .flatMap { pTx -> doValidateAmount(pTx) }
            .flatMap { pTx -> doBuildConfirmations(pTx) }
    }

    companion object {
        const val MINIMUM_CUSTOM_FEE = 1L
        const val maxBTCAmount = 2_100_000_000_000_000L
        const val maxBCHAmount = 2_100_000_000_000_000L
    }
}
