package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.preferences.WalletStatus
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeDetails
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FeeOverRecommended
import piuk.blockchain.android.coincore.FeeTooHigh
import piuk.blockchain.android.coincore.FeeUnderMinLimit
import piuk.blockchain.android.coincore.FeeUnderRecommended
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.ValidCustomFee

abstract class OnChainTxEngineBase(
    override val requireSecondPassword: Boolean,
    private val walletPreferences: WalletStatus
) : TxEngine() {

    override fun assertInputsValid() {
        val tgt = txTarget
        require(tgt is CryptoAddress)
        require(tgt.address.isNotEmpty())
        require(sourceAccount.asset == tgt.asset)
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
                pTx.customFeeAmount == -1L -> ValidCustomFee
                pTx.customFeeAmount < MINIMUM_CUSTOM_FEE -> {
                    FeeUnderMinLimit
                }
                pTx.customFeeAmount >= MINIMUM_CUSTOM_FEE &&
                    pTx.customFeeAmount <= feeOptions?.limits?.min ?: 0L -> {
                    FeeUnderRecommended
                }
                pTx.customFeeAmount >= feeOptions?.limits?.max ?: 0L -> {
                    FeeOverRecommended
                }
                else -> ValidCustomFee
            }
        } else {
            if (pTx.available < pTx.amount) {
                FeeTooHigh
            } else {
                FeeDetails(pTx.fees)
            }
        }

    protected fun updateFeeSelection(
        cryptoCurrency: CryptoCurrency,
        pendingTx: PendingTx,
        newConfirmation: TxConfirmationValue.FeeSelection
    ): Single<PendingTx> {
        setFeeType(cryptoCurrency, newConfirmation.selectedLevel)

        return doUpdateAmount(pendingTx.amount, pendingTx.copy(
            feeLevel = newConfirmation.selectedLevel,
            customFeeAmount = newConfirmation.customFeeAmount
        ))
            .flatMap { pTx -> doValidateAmount(pTx) }
            .flatMap { pTx -> doBuildConfirmations(pTx) }
    }

    companion object {
        const val MINIMUM_CUSTOM_FEE = 1L
        const val maxBTCAmount = 2_100_000_000_000_000L
        const val maxBCHAmount = 2_100_000_000_000_000L
    }
}
