package info.blockchain.wallet.payment

import info.blockchain.wallet.payload.model.Utxo
import info.blockchain.wallet.payment.Fees.estimatedSize
import org.amshove.kluent.`should equal`
import org.junit.Test
import java.math.BigInteger

class FeesTest {

    private val legacyInput = Utxo(value = BigInteger.ZERO, isSegwit = false)
    private val segwitInput = Utxo(value = BigInteger.ZERO, isSegwit = true)

    private val legacyOutput = OutputType.P2PKH
    private val segwitOutput = OutputType.P2WPKH

    @Test
    fun `should return the right transaction size (empty tx)`() {
        estimatedSize(emptyList(), emptyList()).`should equal`(10.0)
    }

    @Test
    fun `should return the right transaction size for script hash outputs`() {
        // 10 + 148 + 32 = 190
        estimatedSize(listOf(legacyInput), listOf(OutputType.P2SH)).`should equal`(190.0)
        // 10 + 148 + 43 = 201
        estimatedSize(listOf(legacyInput), listOf(OutputType.P2WSH)).`should equal`(201.0)
    }

    @Test
    fun `should return the right transaction size (1 input, 1 output)`() {
        // 10 + 148 + 34 = 192
        estimatedSize(listOf(legacyInput), listOf(legacyOutput)).`should equal`(192.0)
        // 10 + 148 + 31 = 189
        estimatedSize(listOf(legacyInput), listOf(segwitOutput)).`should equal`(189.0)
        // 10.75 + 67.75 + 34 = 112.5
        estimatedSize(listOf(segwitInput), listOf(legacyOutput)).`should equal`(112.5)
        // 10.75 + 67.75 + 31 = 109.5
        estimatedSize(listOf(segwitInput), listOf(segwitOutput)).`should equal`(109.5)
    }

    @Test
    fun `should return the right transaction size (1 input, 2 outputs)`() {
        // 10 + 148 + 34*2 = 226
        estimatedSize(listOf(legacyInput), listOf(legacyOutput, legacyOutput)).`should equal`(226.0)
        // 10 + 148 + 31*2 = 220
        estimatedSize(listOf(legacyInput), listOf(segwitOutput, segwitOutput)).`should equal`(220.0)
        // 10 + 148 + 31 + 34 = 223
        estimatedSize(listOf(legacyInput), listOf(legacyOutput, segwitOutput)).`should equal`(223.0)
        // 10.75 + 67.75 + 34*2 = 146.5
        estimatedSize(listOf(segwitInput), listOf(legacyOutput, legacyOutput)).`should equal`(146.5)
        // 10.75 + 67.75 + 31*2 = 140.5
        estimatedSize(listOf(segwitInput), listOf(segwitOutput, segwitOutput)).`should equal`(140.5)
        // 10.75 + 67.75 + 31 + 34 = 143.5
        estimatedSize(listOf(segwitInput), listOf(legacyOutput, segwitOutput)).`should equal`(143.5)
    }

    @Test
    fun `should return the right transaction size (2 inputs, 1 output)`() {
        // 10 + 148*2 + 34 = 340
        estimatedSize(listOf(legacyInput, legacyInput), listOf(legacyOutput)).`should equal`(340.0)
        // 10 + 148*2 + 31 = 337
        estimatedSize(listOf(legacyInput, legacyInput), listOf(segwitOutput)).`should equal`(337.0)
        // 10.75 + 67.75 + 148 + 34 = 260.5
        estimatedSize(listOf(legacyInput, segwitInput), listOf(legacyOutput)).`should equal`(260.5)
        // 10.75 + 67.75*2 + 34 = 180.25
        estimatedSize(listOf(segwitInput, segwitInput), listOf(legacyOutput)).`should equal`(180.25)
        // 10.75 + 67.75*2 + 31 = 177.25
        estimatedSize(listOf(segwitInput, segwitInput), listOf(segwitOutput)).`should equal`(177.25)
        // 10.75 + 67.75 + 148 + 31 = 177.25
        estimatedSize(listOf(legacyInput, segwitInput), listOf(segwitOutput)).`should equal`(257.5)
    }

    @Test
    fun `should return the right transaction size (2 inputs, 2 outputs)`() {
        // 10 + 148*2 + 34*2 = 374
        estimatedSize(listOf(legacyInput, legacyInput), listOf(legacyOutput, legacyOutput)).`should equal`(374.0)
        // 10 + 148*2 + 31*2 = 368
        estimatedSize(listOf(legacyInput, legacyInput), listOf(segwitOutput, segwitOutput)).`should equal`(368.0)
        // 10.75 + 148 + 67.75 + 34*2 = 294.5
        estimatedSize(listOf(legacyInput, segwitInput), listOf(legacyOutput, legacyOutput)).`should equal`(294.5)
        // 10 + 148*2 + 31 + 34 = 371
        estimatedSize(listOf(legacyInput, legacyInput), listOf(legacyOutput, segwitOutput)).`should equal`(371.0)
        // 10.75 + 67.75 + 148 + 31 + 34 = 291.5
        estimatedSize(listOf(legacyInput, segwitInput), listOf(legacyOutput, segwitOutput)).`should equal`(291.5)
        // 10.75 + 67.75*2 + 34*2 = 214.25
        estimatedSize(listOf(segwitInput, segwitInput), listOf(legacyOutput, legacyOutput)).`should equal`(214.25)
        // 10.75 + 67.75*2 + 31*2 = 208.25
        estimatedSize(listOf(segwitInput, segwitInput), listOf(segwitOutput, segwitOutput)).`should equal`(208.25)
        // 10.75 + 67.75*2 + 31 + 34 = 211.25
        estimatedSize(listOf(segwitInput, segwitInput), listOf(legacyOutput, segwitOutput)).`should equal`(211.25)
        // 10.75 + 67.75 + 148 + 31*2 = 288.5
        estimatedSize(listOf(legacyInput, segwitInput), listOf(segwitOutput, segwitOutput)).`should equal`(288.5)
    }
}