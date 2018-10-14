package com.blockchain.sunriver.ed25519

import org.amshove.kluent.`should equal`
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Arrays

/**
 * Test cases from SLIP-0010 https://github.com/satoshilabs/slips/blob/master/slip-0010.md
 */
class SLIP0010TestCases {

    @Test
    fun `case 1`() {
        "000102030405060708090a0b0c0d0e0f"
            .testDerive() `should equal` "2b4be7f19ee27bbf30c667b642d5f4aa69fd169872f8fc3059c08ebae2eb19e7"
    }
}

private fun String.testDerive(): String {
    return hexToBytes().derive().toHex()
}

private fun ByteArray.derive(): ByteArray {
    val I = hmacSha512("ed25519 seed".toByteArray(StandardCharsets.UTF_8), this)


    return I.head32()
}

private fun ByteArray.head32(): ByteArray {
    return Arrays.copyOfRange(this, 0, 32)
}
