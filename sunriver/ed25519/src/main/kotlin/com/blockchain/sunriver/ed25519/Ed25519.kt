package com.blockchain.sunriver.ed25519

import java.nio.charset.Charset
import java.util.Arrays

/**
 * Ed25519 derivation according to SLIP-0010
 * https://github.com/satoshilabs/slips/blob/master/slip-0010.md
 *
 *  - Only hardened key derivation is possible with ED25519, for this reason, this function hardens indexes for you.
 *  - For Ed25519 every 256-bit number (even 0) is a valid private key, so there are no hard to find and test edge
 *  cases, such as "parse256(IL) ≥ n or parse256(IL) + kpar (mod n) = 0" with secp256k1.
 *
 * @indexes These ints can be hardened or not, if they are not, they will be hardened by this function.
 */
@Suppress("LocalVariableName")
fun deriveEd25519PrivateKey(seed: ByteArray, vararg indexes: Int): ByteArray {

    var I = hmacSha512("ed25519 seed".toByteArray(Charset.forName("UTF-8")), seed)

    val Il = ByteArray(32)
    val Ir = ByteArray(32)
    val data = ByteArray(37)

    I putHead32Into Il

    indexes.forEach { index ->

        I putTail32Into Ir

        Il put32BytesIntoOffset1 data
        index.hard() put4BytesIntoOffset33 data

        I.clear()
        I = hmacSha512(Ir, data)
        data.clear(); Ir.clear()

        I putHead32Into Il
    }

    I.clear()
    return Il
}

/**
 * Writes a 32 bit int [i32] to a byte array starting [atIndex]
 */
private fun ByteArray.ser32(i32: Int, atIndex: Int) {
    this[atIndex] = (i32 shr 24).toByte()
    this[atIndex + 1] = (i32 shr 16).toByte()
    this[atIndex + 2] = (i32 shr 8).toByte()
    this[atIndex + 3] = i32.toByte()
}

private infix fun ByteArray.putHead32Into(into: ByteArray) = System.arraycopy(this, 0, into, 0, 32)

private infix fun ByteArray.put32BytesIntoOffset1(into: ByteArray) = System.arraycopy(this, 0, into, 1, 32)
private infix fun Int.put4BytesIntoOffset33(into: ByteArray) = into.ser32(this, 33)
private infix fun ByteArray.putTail32Into(into: ByteArray) = System.arraycopy(this, 32, into, 0, 32)

private fun ByteArray.clear() = Arrays.fill(this, 0)

private fun Int.hard() = this or -0x80000000
