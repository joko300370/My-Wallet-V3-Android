package info.blockchain.wallet.payment

import java.math.BigInteger
import kotlin.math.roundToInt

enum class OutputType(val size: Double) {
    P2PKH(34.0),
    P2WPKH(31.0),
    P2SH(32.0),
    P2WSH(43.0);

    val cost: BigInteger
        get() = size.roundToInt().toBigInteger()
}