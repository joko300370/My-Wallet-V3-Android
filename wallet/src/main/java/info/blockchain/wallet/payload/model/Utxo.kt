package info.blockchain.wallet.payload.model

import info.blockchain.api.bitcoin.data.UnspentOutputDto
import info.blockchain.api.bitcoin.data.XpubDto
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.data.XPubs
import java.math.BigInteger

data class Utxo(
    val value: BigInteger,
    val script: String = "",
    val txHash: String = "",
    val txOutputCount: Int = 0,
    val isReplayable: Boolean = true,
    val xpub: XpubDto? = null,
    val isSegwit: Boolean = false,
    var isForceInclude: Boolean = false
)

fun UnspentOutputDto.toBtcUtxo(xpubs: XPubs): Utxo {
    return Utxo(
        value = BigInteger(value),
        script = script ?: "",
        txHash = txHash,
        txOutputCount = txOutputCount,
        isReplayable = replayable,
        xpub = xpub,
        isSegwit = isSegwitOutput(xpubs, xpub?.address),
        isForceInclude = false
    )
}

fun UnspentOutputDto.toBchUtxo(): Utxo {
    return Utxo(
        value = BigInteger(value),
        script = script ?: "",
        txHash = txHash,
        txOutputCount = txOutputCount,
        isReplayable = replayable,
        xpub = xpub,
        isSegwit = false,
        isForceInclude = false
    )
}

private fun isSegwitOutput(xpubs: XPubs, outputXpub: String?): Boolean {
    val segwitInputXpub = xpubs.forDerivation(XPub.Format.SEGWIT)
    return when {
        outputXpub == null -> false
        segwitInputXpub == null -> false
        segwitInputXpub.address == outputXpub -> true
        else -> false
    }
}