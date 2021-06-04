package info.blockchain.wallet.payment

import info.blockchain.wallet.payload.model.Utxo
import info.blockchain.wallet.payment.Payment.Companion.PUSHTX_MIN
import java.math.BigInteger
import kotlin.math.ceil

internal object Fees {
    // Legacy (P2PKH)
    private const val ESTIMATED_OVERHEAD_LEN = 10
    private const val ESTIMATED_INPUT_LEN = 148 // compressed key

    // Segwit (P2WPKH)
    private const val ESTIMATED_SEGWIT_OVERHEAD_LEN = 10.75
    private const val ESTIMATED_SEGWIT_INPUT_LEN = 67.75

    fun estimatedFee(inputs: List<Utxo>, outputs: List<OutputType>, feePerKb: BigInteger): BigInteger {
        val txBytes = estimatedSize(inputs, outputs)
        return calculateFee(txBytes, feePerKb)
    }

    fun estimatedSize(inputs: List<Utxo>, outputs: List<OutputType>): Double {
        val overheadBytes = overheadBytes(inputs)
        val inputTotal = inputs.fold(0.0) { acc, utxo -> acc + inputBytes(utxo) }
        val outputTotal = outputs.fold(0.0) { acc, type -> acc + type.size }

        return overheadBytes + inputTotal + outputTotal
    }

    fun isAdequateFee(inputs: List<Utxo>, outputs: List<OutputType>, absoluteFee: BigInteger): Boolean {
        val txBytes = estimatedSize(inputs, outputs) / 1000.0
        val feePerkb = ceil(absoluteFee.toDouble() / txBytes).toLong()
        return feePerkb > PUSHTX_MIN.toLong()
    }

    fun calculateFee(size: Double, feePerKb: BigInteger): BigInteger {
        val txBytes = size / 1000.0
        val absoluteFee = ceil(feePerKb.toDouble() * txBytes).toLong()
        return BigInteger.valueOf(absoluteFee)
    }

    fun inputCost(input: Utxo, feePerKb: BigInteger): Double {
        val inputBytes = inputBytes(input) / 1000.0
        return ceil(feePerKb.toDouble() * inputBytes)
    }

    private fun overheadBytes(inputs: List<Utxo>): Double {
        return when (inputs.any { it.isSegwit }) {
            true -> ESTIMATED_SEGWIT_OVERHEAD_LEN
            else -> ESTIMATED_OVERHEAD_LEN.toDouble()
        }
    }

    private fun inputBytes(input: Utxo): Double {
        return when (input.isSegwit) {
            true -> ESTIMATED_SEGWIT_INPUT_LEN
            else -> ESTIMATED_INPUT_LEN.toDouble()
        }
    }
}