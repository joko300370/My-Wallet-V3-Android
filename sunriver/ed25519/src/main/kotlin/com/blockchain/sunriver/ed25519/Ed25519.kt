package com.blockchain.sunriver.ed25519

import java.nio.charset.Charset
import java.util.Arrays
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Ed25519 derivation according to SLIP-0010
 * https://github.com/satoshilabs/slips/blob/master/slip-0010.md
 *
 *  - Only hardened key derivation is possible with ED25519, for this reason, this function hardens indexes for you.
 *  - For Ed25519 every 256-bit number (even 0) is a valid private key, so there are no hard to find and test edge
 *  cases, such as "parse256(IL) ≥ n or parse256(IL) + kpar (mod n) = 0" with secp256k1.
 *
 * @seed This is the seed that comes from BIP0039
 * @indexes Child indexes for the path. E.g. For path: m/1'/2'/3' will be 1, 2, 3.
 * These ints can be hardened or not, they will be hardened by this function regardless.
 */
@Suppress("LocalVariableName")
fun deriveEd25519PrivateKey(seed: ByteArray, vararg indexes: Int): ByteArray {

    val Il = ByteArray(32)
    val Ir = ByteArray(32)
    val data = ByteArray(37)

    var I = hmacSha512("ed25519 seed".toByteArray(Charset.forName("UTF-8")), seed)

    I putHead32BytesInto Il

    indexes.forEach { index ->

        I putTail32BytesInto Ir
        I.clear()

        data[0] = 0
        Il put32BytesIntoOffset1 data
        index.hard() put4BytesIntoOffset33 data

        I = hmacSha512(Ir, data)
        data.clear(); Ir.clear()

        I putHead32BytesInto Il
    }

    I.clear()
    return Il
}

private infix fun ByteArray.putHead32BytesInto(into: ByteArray) = System.arraycopy(this, 0, into, 0, 32)

private infix fun ByteArray.putTail32BytesInto(into: ByteArray) = System.arraycopy(this, 32, into, 0, 32)

private infix fun ByteArray.put32BytesIntoOffset1(into: ByteArray) = System.arraycopy(this, 0, into, 1, 32)

private infix fun Int.put4BytesIntoOffset33(into: ByteArray) = into.ser32(this, 33)

/**
 * Writes a 32 bit int [i32] to a byte array starting [atIndex]
 */
private fun ByteArray.ser32(i32: Int, atIndex: Int) {
    this[atIndex] = (i32 shr 24).toByte()
    this[atIndex + 1] = (i32 shr 16).toByte()
    this[atIndex + 2] = (i32 shr 8).toByte()
    this[atIndex + 3] = i32.toByte()
}

private fun ByteArray.clear() = Arrays.fill(this, 0)

private fun Int.hard() = this or -0x80000000

private const val HMAC_SHA512 = "HmacSHA512"

private fun hmacSha512(byteKey: ByteArray, seed: ByteArray) =
    Mac.getInstance(HMAC_SHA512).apply {
        init(SecretKeySpec(byteKey, HMAC_SHA512))
    }.doFinal(seed)
