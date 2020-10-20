package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.preferences.WalletStatus
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeDetails
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FeeTooHigh
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.coincore.TxResult

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

    protected fun setFeeType(cryptoCurrency: CryptoCurrency, feeLevel: FeeLevel) =
        walletPreferences.setFeeTypeForAsset(cryptoCurrency, feeLevel.mapFeeLevelToSavedValue())

    protected fun getFeeType(cryptoCurrency: CryptoCurrency): Int? =
        walletPreferences.getFeeTypeForAsset(cryptoCurrency)

    protected fun getFeeState(fees: Money, amount: Money, available: Money) =
        if (available < amount) {
            FeeTooHigh
        } else {
            FeeDetails(fees)
        }

    protected fun updateFeeSelection(
        cryptoCurrency: CryptoCurrency,
        pendingTx: PendingTx,
        newOption: TxOptionValue.FeeSelection
    ): Single<PendingTx> {
        setFeeType(cryptoCurrency, newOption.selectedLevel)

        return doUpdateAmount(pendingTx.amount, pendingTx.copy(feeLevel = newOption.selectedLevel))
            .flatMap { pTx -> doValidateAmount(pTx) }
            .flatMap { pTx -> doBuildConfirmations(pTx) }
    }

    companion object {
        const val maxBTCAmount = 2_100_000_000_000_000L
        const val maxBCHAmount = 2_100_000_000_000_000L
    }
}
