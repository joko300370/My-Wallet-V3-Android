package com.blockchain.sunriver.ed25519

import java.nio.charset.StandardCharsets
import java.util.Arrays

@Suppress("LocalVariableName")
fun ByteArray.derivePrivateKey(vararg indexes: Int): ByteArray {
    var I = hmacSha512("ed25519 seed".toByteArray(StandardCharsets.UTF_8), this)

    val Il = ByteArray(32)
    val Ir = ByteArray(32)
    val data = ByteArray(37)

    I putHead32Into Il

    indexes.forEach { index ->
        I putTail32Into Ir

        Il put32BytesIntoOffset1 data
        index put4BytesIntoOffset33 data

        I.clear()
        I = hmacSha512(Ir, data)
        data.clear(); Ir.clear()

        I putHead32Into Il
    }

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
