package piuk.blockchain.android.coincore.impl.txEngine

import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.TxEngine

abstract class OnChainTxEngineBase(
    override val requireSecondPassword: Boolean
) : TxEngine() {

    override fun assertInputsValid() {
        val tgt = txTarget
        require(tgt is CryptoAddress)
        require(tgt.address.isNotEmpty())
        require(sourceAccount.asset == tgt.asset)
    }
}
