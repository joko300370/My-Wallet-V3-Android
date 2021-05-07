package info.blockchain.wallet.payment

import info.blockchain.wallet.payload.model.Utxo

import java.math.BigInteger

class SpendableUnspentOutputs(
    var spendableOutputs: List<Utxo> = emptyList(),
    var absoluteFee: BigInteger = BigInteger.ZERO,
    var consumedAmount: BigInteger = BigInteger.ZERO,
    var isReplayProtected: Boolean = false
) {
    val spendableBalance get() = spendableOutputs.sum() - absoluteFee
}
