package info.blockchain.wallet.payment

import info.blockchain.wallet.payload.model.Utxo
import java.math.BigInteger

private val COST_BASE: BigInteger = BigInteger.valueOf(10)
private val COST_PER_INPUT: BigInteger = BigInteger.valueOf(149)
private val COST_PER_OUTPUT: BigInteger = BigInteger.valueOf(34)

class CoinSelection(
    private val coins: List<Utxo>,
    private val feePerByte: BigInteger
) {
    fun select(
        outputAmount: BigInteger,
        coinSortingMethod: CoinSortingMethod
    ): SpendableUnspentOutputs {
        val effectiveCoins = coinSortingMethod.sort(coins).effective(feePerByte)

        val selected = mutableListOf<Utxo>()
        var accumulatedValue = BigInteger.ZERO
        var accumulatedFee = BigInteger.ZERO

        for (coin in effectiveCoins) {
            if (!coin.isForceInclude && accumulatedValue >= outputAmount + accumulatedFee) {
                continue
            }
            selected += coin
            accumulatedValue = selected.sum()
            accumulatedFee = transactionBytes(selected.size, outputs = 1) * feePerByte
        }

        val dust = dustThreshold(feePerByte)
        val remainingValue = accumulatedValue - (outputAmount + accumulatedFee)
        val isReplayProtected = selected.replayProtected

        return when {
            // Either there were no effective coins or we were not able to meet the target value
            selected.isEmpty() || remainingValue < BigInteger.ZERO -> {
                SpendableUnspentOutputs(isReplayProtected = isReplayProtected)
            }
            // Remaining value is worth keeping, add change output
            remainingValue >= dust -> {
                accumulatedFee = transactionBytes(selected.size, outputs = 2) * feePerByte
                SpendableUnspentOutputs(selected, accumulatedFee, isReplayProtected = isReplayProtected)
            }
            // Remaining value is not worth keeping, consume it as part of the fee
            else -> {
                SpendableUnspentOutputs(selected, accumulatedFee + remainingValue, remainingValue, isReplayProtected)
            }
        }
    }

    fun selectAll(coinSortingMethod: CoinSortingMethod? = null): SpendableUnspentOutputs {
        val effectiveCoins = (coinSortingMethod?.sort(coins) ?: coins).effective(feePerByte)
        val effectiveValue = effectiveCoins.sum()
        val effectiveBalance = effectiveCoins.balance(feePerByte, outputs = 1).max(BigInteger.ZERO)

        return SpendableUnspentOutputs(
            spendableOutputs = effectiveCoins,
            absoluteFee = effectiveValue - effectiveBalance,
            isReplayProtected = effectiveCoins.replayProtected
        )
    }
}

fun List<Utxo>.sum(): BigInteger {
    if (isEmpty()) {
        return BigInteger.ZERO
    }
    return this.map { it.value }.reduce { value, acc -> value + acc }
}

private fun List<Utxo>.effective(feePerByte: BigInteger): List<Utxo> {
    return this.filter { it.isForceInclude || effectiveValue(it, feePerByte) > BigInteger.ZERO }
}

private fun List<Utxo>.balance(feePerByte: BigInteger, outputs: Int): BigInteger {
    return this.sum() - transactionBytes(this.size, outputs) * feePerByte
}

private val List<Utxo>.replayProtected get(): Boolean {
    return this.firstOrNull()?.isReplayable != true
}

private fun dustThreshold(feePerByte: BigInteger): BigInteger {
    return (COST_PER_INPUT + COST_PER_OUTPUT) * feePerByte
}

private fun transactionBytes(inputs: Int, outputs: Int): BigInteger {
    return COST_BASE +
            COST_PER_INPUT.multiply(inputs.toBigInteger()) +
            COST_PER_OUTPUT.multiply(outputs.toBigInteger())
}

private fun effectiveValue(coin: Utxo, feePerByte: BigInteger): BigInteger {
    return (coin.value - feePerByte.multiply(COST_PER_INPUT)).max(BigInteger.ZERO)
}
