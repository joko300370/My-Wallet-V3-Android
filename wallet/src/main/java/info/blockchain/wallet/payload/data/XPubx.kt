package info.blockchain.wallet.payload.data

import java.lang.IllegalStateException

data class XPubs(
    private val xpubs: List<XPub>
) {
    val default: XPub
        get() = xpubs.firstOrNull { it.derivation == XPub.Format.SEGWIT }
            ?: xpubs.firstOrNull { it.derivation == XPub.Format.LEGACY }
            ?: throw IllegalStateException("Xpub format not found")

    fun forDerivation(format: XPub.Format): XPub? =
        xpubs.firstOrNull { it.derivation == format }

    fun allAddresses(): List<String> =
        xpubs.map { it.address }

    constructor(xpub: XPub) : this(listOf(xpub))
}

data class XPub(
    val address: String,
    val derivation: Format
) {
    enum class Format {
        LEGACY,
        SEGWIT
    }
}

fun List<XPubs>.segwitXpubAddresses() =
    mapNotNull { it.forDerivation(XPub.Format.SEGWIT) }.map { it.address }

fun List<XPubs>.legacyXpubAddresses() =
    mapNotNull { it.forDerivation(XPub.Format.LEGACY) }.map { it.address }

fun List<XPubs>.allAddresses(): List<String> =
    map { it.allAddresses() }.flatten()