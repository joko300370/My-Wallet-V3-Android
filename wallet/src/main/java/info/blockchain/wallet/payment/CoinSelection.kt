package info.blockchain.wallet.payment

import info.blockchain.wallet.payload.model.Utxo
import java.math.BigInteger

private val COST_BASE: BigInteger = BigInteger.valueOf(10)
private val COST_PER_INPUT_LEGACY: BigInteger = BigInteger.valueOf(149)
private val COST_PER_INPUT_SEGWIT: BigInteger = BigInteger.valueOf(68)

class CoinSelection(
    private val coins: List<Utxo>,
    private val feePerByte: BigInteger
) {
    fun select(
        outputAmount: BigInteger,
        targetOutputType: OutputType,
        changeOutputType: OutputType,
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
            accumulatedFee = transactionBytes(selected, outputs = 1, targetOutputType) * feePerByte
        }

        val dust = dustThreshold(changeOutputType, feePerByte)
        val remainingValue = accumulatedValue - (outputAmount + accumulatedFee)
        val isReplayProtected = selected.replayProtected

        return when {
            // Either there were no effective coins or we were not able to meet the target value
            selected.isEmpty() || remainingValue < BigInteger.ZERO -> {
                SpendableUnspentOutputs(isReplayProtected = isReplayProtected)
            }
            // Remaining value is worth keeping, add change output
            remainingValue >= dust -> {
                accumulatedFee = transactionBytes(selected, outputs = 2, changeOutputType) * feePerByte
                SpendableUnspentOutputs(selected, accumulatedFee, isReplayProtected = isReplayProtected)
            }
            // Remaining value is not worth keeping, consume it as part of the fee
            else -> {
                SpendableUnspentOutputs(
                    selected,
                    accumulatedFee + remainingValue,
                    remainingValue,
                    isReplayProtected

                )
            }
        }
    }

    fun selectAll(
        targetOutputType: OutputType,
        coinSortingMethod: CoinSortingMethod? = null
    ): SpendableUnspentOutputs {
        val effectiveCoins = (coinSortingMethod?.sort(coins) ?: coins).effective(feePerByte)
        val effectiveValue = effectiveCoins.sum()
        val effectiveBalance = effectiveCoins.balance(feePerByte, targetOutputType, outputs = 1).max(BigInteger.ZERO)

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

private fun List<Utxo>.balance(
    feePerByte: BigInteger,
    outputType: OutputType,
    outputs: Int
): BigInteger {
    return this.sum() - transactionBytes(this, outputs, outputType) * feePerByte
}

private val List<Utxo>.replayProtected get(): Boolean {
    return this.firstOrNull()?.isReplayable != true
}

private fun dustThreshold(outputType: OutputType, feePerByte: BigInteger): BigInteger =
    when (outputType) {
        OutputType.P2PKH,
        OutputType.P2SH -> (COST_PER_INPUT_LEGACY + outputType.cost) * feePerByte
        else -> (COST_PER_INPUT_SEGWIT + outputType.cost) * feePerByte
    }

private fun transactionBytes(
    inputs: List<Utxo>,
    outputs: Int,
    outputType: OutputType
): BigInteger {
    val segwitInputs = inputs.count { it.isSegwit }
    val legacyInputs = inputs.size - segwitInputs

    return COST_BASE +
            COST_PER_INPUT_LEGACY.multiply(legacyInputs.toBigInteger()) +
            COST_PER_INPUT_SEGWIT.multiply(segwitInputs.toBigInteger()) +
            outputType.cost.multiply(outputs.toBigInteger())
}

private fun effectiveValue(coin: Utxo, feePerByte: BigInteger): BigInteger {
    val costPerInput = when (coin.isSegwit) {
        true -> COST_PER_INPUT_SEGWIT
        false -> COST_PER_INPUT_LEGACY
    }
    return (coin.value - feePerByte.multiply(costPerInput)).max(BigInteger.ZERO)
}
