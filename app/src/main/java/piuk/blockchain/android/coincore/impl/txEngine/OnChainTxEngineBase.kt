package piuk.blockchain.android.coincore.impl.txEngine

import io.reactivex.Completable
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.TxResult

abstract class OnChainTxEngineBase(
    override val requireSecondPassword: Boolean
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

    protected fun FeeLevel.mapFeeLevelToSavedValue() =
        this.ordinal
}
