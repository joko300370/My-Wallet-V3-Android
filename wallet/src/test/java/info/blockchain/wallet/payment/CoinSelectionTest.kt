package info.blockchain.wallet.payment

import info.blockchain.wallet.payload.model.Utxo
import org.amshove.kluent.`should equal`
import org.junit.Test
import java.math.BigInteger

class CoinSelectionTest {
    private fun unspent(value: Int) = Utxo(value = value.toBigInteger())

    private fun unspents(vararg unspents: Int) = unspents.map { unspent(it) }

    private fun List<Utxo>.values() = map { it.value }

    private val feePerByte = 55.toBigInteger()

    @Test
    fun `ascent draw selection with change output`() {
        val coins = unspents(1, 20000, 0, 0, 300000, 50000, 30000)
        val outputAmount = 100000.toBigInteger()

        CoinSelection(coins, feePerByte)
            .select(
                outputAmount = outputAmount,
                targetOutputType = OutputType.P2PKH,
                changeOutputType = OutputType.P2PKH,
                coinSortingMethod = AscentDraw
            )
            .also {
                it.spendableOutputs.values() `should equal` unspents(20000, 30000, 50000, 300000).values()
                it.absoluteFee `should equal` 37070.toBigInteger()
                it.consumedAmount `should equal` BigInteger.ZERO
            }
    }

    @Test
    fun `ascent draw selection with no change output`() {
        val coins = unspents(200000, 300000, 500000)
        val selected = unspents(200000, 300000)
        val outputAmount = 472000.toBigInteger()

        CoinSelection(coins, feePerByte).select(
            outputAmount = outputAmount,
            targetOutputType = OutputType.P2PKH,
            changeOutputType = OutputType.P2PKH,
            coinSortingMethod = AscentDraw
        ).also {
            it.spendableOutputs.values() `should equal` selected.values()
            it.absoluteFee `should equal` (selected.sum() - outputAmount)
            it.consumedAmount `should equal` 9190.toBigInteger()
        }
    }

    @Test
    fun `descent draw selection with change output`() {
        val coins = unspents(1, 20000, 0, 0, 300000, 50000, 30000)
        val outputAmount = 100000.toBigInteger()

        CoinSelection(coins, feePerByte).select(
            outputAmount = outputAmount,
            targetOutputType = OutputType.P2PKH,
            changeOutputType = OutputType.P2PKH,
            coinSortingMethod = DescentDraw
        ).also {
            it.spendableOutputs.values() `should equal` unspents(300000).values()
            it.absoluteFee `should equal` 12485.toBigInteger()
            it.consumedAmount `should equal` BigInteger.ZERO
        }
    }

    @Test
    fun `descent draw selection with no change output`() {
        val coins = unspents(200000, 300000, 500000)
        val selected = unspents(500000)
        val outputAmount = 485000.toBigInteger()

        CoinSelection(coins, feePerByte).select(
            outputAmount = outputAmount,
            targetOutputType = OutputType.P2PKH,
            changeOutputType = OutputType.P2PKH,
            coinSortingMethod = DescentDraw
        ).also {
            it.spendableOutputs.values() `should equal` selected.values()
            it.absoluteFee `should equal` (selected.sum() - outputAmount)
            it.consumedAmount `should equal` 4385.toBigInteger()
        }
    }

    @Test
    fun `select all selection with effective inputs`() {
        val coins = unspents(1, 20000, 0, 0, 300000)

        CoinSelection(coins, feePerByte).selectAll(OutputType.P2PKH).also {
            it.spendableOutputs.values() `should equal` unspents(20000, 300000).values()
            it.absoluteFee `should equal` 18810.toBigInteger()
            it.consumedAmount `should equal` BigInteger.ZERO
        }
    }

    @Test
    fun `select all selection with no inputs`() {
        val coins = unspents()

        CoinSelection(coins, feePerByte).selectAll(OutputType.P2PKH).also {
            it.spendableOutputs.values() `should equal` unspents().values()
            it.absoluteFee `should equal` 0.toBigInteger()
            it.consumedAmount `should equal` BigInteger.ZERO
        }
    }

    @Test
    fun `select all selection with no effective inputs`() {
        val coins = unspents(1, 10, 100)

        CoinSelection(coins, feePerByte).selectAll(OutputType.P2PKH).also {
            it.spendableOutputs.values() `should equal` unspents().values()
            it.absoluteFee `should equal` 0.toBigInteger()
            it.consumedAmount `should equal` BigInteger.ZERO
        }
    }
}