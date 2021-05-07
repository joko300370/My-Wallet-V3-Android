package info.blockchain.wallet.payment

import info.blockchain.wallet.payload.model.Utxo
import org.bitcoinj.script.Script
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.Collections

internal object Coins {

    // Size added to combined tx using dust-service to approximate fee
    private val log = LoggerFactory.getLogger(Coins::class.java)

    // Size added to combined tx using dust-service to approximate fee
    private const val DUST_INPUT_TX_SIZE_ADAPT = 150

    private val placeholderDustInput: Utxo
        get() {
            return Utxo(
                value = Payment.DUST,
                isForceInclude = true
            )
        }

    /**
     * Computes the available amount to send and the associated fee in satoshis provided a list of
     * coins and the fee per Kilobyte.
     *
     * @param utxoList the UTXOs
     * @param targetOutputType Destination output type
     * @param feePerKb the fee per KB
     * @param addReplayProtection boolean whether replay protection should be considered
     * @return a Pair of maximum available amount to send and the associated fee in satoshis.
     */
    fun getMaximumAvailable(
        utxoList: List<Utxo>,
        targetOutputType: OutputType,
        feePerKb: BigInteger,
        addReplayProtection: Boolean,
        useNewCoinSelection: Boolean
    ): Pair<BigInteger, BigInteger> {
        if (useNewCoinSelection) {
            var coinSortingMethod: CoinSortingMethod? = null

            if (addReplayProtection) {
                coinSortingMethod = ReplayProtection(placeholderDustInput)
            }

            val selection = CoinSelection(utxoList, feePerKbToFeePerByte(feePerKb))
                    .selectAll(coinSortingMethod)

            return Pair(selection.spendableBalance, selection.absoluteFee)
        }

        var sweepBalance = BigInteger.ZERO

        val usableCoins = ArrayList<Utxo>()
        val unspentOutputs: MutableList<Utxo>

        // Sort inputs
        if (addReplayProtection) {
            unspentOutputs = getSortedCoins(utxoList)
        } else {
            unspentOutputs = utxoList.toMutableList()
            Collections.sort(unspentOutputs, UnspentOutputAmountComparatorDesc())
        }

        val includesReplayDust =
            addReplayProtection && requiresReplayProtection(unspentOutputs)

        if (includesReplayDust) {
            log.info("Calculating maximum available with non-replayable dust included.")
            unspentOutputs.add(0, placeholderDustInput)
        }

        for (i in unspentOutputs.indices) {
            val output = unspentOutputs[i]
            val inputCost = Fees.inputCost(unspentOutputs[i], feePerKb)

            // Filter usable coins
            if (output.isForceInclude || output.value.toDouble() > inputCost) {
                usableCoins.add(output)
                sweepBalance = sweepBalance.add(output.value)
            }
        }

        // All inputs, 1 output = no change. (Correct way)
        val sweepFee = calculateFee(
            inputs = usableCoins,
            outputs = listOf(targetOutputType),
            feePerKb = feePerKb,
            includesReplayDust = includesReplayDust
        )

        sweepBalance = sweepBalance.subtract(sweepFee)

        sweepBalance = BigInteger.valueOf(Math.max(sweepBalance.toLong(), 0))

        log.info("Filtering sweepable coins. Sweepable Balance = {}, Fee required for sweep = {}",
            sweepBalance,
            sweepFee)
        return Pair(sweepBalance, sweepFee)
    }

    /**
     * Sort in order - 1 smallest non-replayable coin, descending replayable, descending
     * non-replayable
     */
    private fun getSortedCoins(utxoList: List<Utxo>): MutableList<Utxo> {
        val sortedCoins = ArrayList<Utxo>()

        // Select 1 smallest non-replayable coin
        Collections.sort(utxoList, UnspentOutputAmountComparatorAsc())
        for (coin in utxoList) {
            if (!coin.isReplayable) {
                coin.isForceInclude = true
                sortedCoins.add(coin)
                break
            }
        }

        // Descending value. Add all replayable coins.
        val reversed = utxoList.toMutableList()
        reversed.reverse()

        reversed.forEach { coin ->
            if (!sortedCoins.contains(coin) && coin.isReplayable) {
                sortedCoins.add(coin)
            }
        }

        // Still descending. Add all non-replayable coins.
        reversed.forEach { coin ->
            if (!sortedCoins.contains(coin) && !coin.isReplayable) {
                sortedCoins.add(coin)
            }
        }
        return sortedCoins
    }

    /**
     * Returns the spendable coins provided the desired amount to send.
     *
     * @param utxoList a list of coins
     * @param targetOutputType Destination output type
     * @param changeOutputType Change output type
     * @param paymentAmount the desired amount to send
     * @param feePerKb he fee per KB
     * @param addReplayProtection whether or no replay protection should be considered
     * @return a list of spendable coins
     */
    fun getMinimumCoinsForPayment(
        utxoList: List<Utxo>,
        targetOutputType: OutputType,
        changeOutputType: OutputType,
        paymentAmount: BigInteger,
        feePerKb: BigInteger,
        addReplayProtection: Boolean,
        useNewCoinSelection: Boolean
    ): SpendableUnspentOutputs {
        if (useNewCoinSelection) {
            val coinSortingMethod: CoinSortingMethod = if (addReplayProtection) {
                ReplayProtection(placeholderDustInput)
            } else {
                DescentDraw
            }

            return CoinSelection(utxoList, feePerKbToFeePerByte(feePerKb))
                .select(paymentAmount, coinSortingMethod)
        }

        log.info("Select the minimum number of outputs necessary for payment")
        val spendWorthyList = ArrayList<Utxo>()

        val unspentOutputs: MutableList<Utxo>

        // Sort inputs
        if (addReplayProtection) {
            unspentOutputs = getSortedCoins(utxoList)
        } else {
            unspentOutputs = utxoList.toMutableList()
            Collections.sort(unspentOutputs, UnspentOutputAmountComparatorDesc())
        }

        var collectedAmount = BigInteger.ZERO
        var consumedAmount = BigInteger.ZERO

        val requiresReplayProtection = requiresReplayProtection(unspentOutputs)
        val includesReplayDust = addReplayProtection && requiresReplayProtection
        if (includesReplayDust) {
            log.info("Adding non-replayable dust to selected coins.")
            unspentOutputs.add(0, placeholderDustInput)
        }

        // initially assume change
        var assumeChange = true

        for (i in unspentOutputs.indices) {
            val output = unspentOutputs[i]
            val inputCost = Fees.inputCost(output, feePerKb)

            // Filter coins not worth spending
            if (output.value.toDouble() < inputCost && !output.isForceInclude) {
                continue
            }

            // Skip script with no type
            if (!output.isForceInclude &&
                Script(Hex.decode(output.script.toByteArray())).scriptType == null
            ) {
                continue
            }

            // Collect coin
            spendWorthyList.add(output)
            collectedAmount = collectedAmount.add(output.value)

            // Fee
            val paymentAmountNoChange = estimateAmount(
                inputs = spendWorthyList,
                outputs = listOf(targetOutputType),
                paymentAmount = paymentAmount,
                feePerKb = feePerKb
            )

            val paymentAmountWithChange = estimateAmount(
                inputs = spendWorthyList,
                outputs = listOf(targetOutputType, changeOutputType),
                paymentAmount = paymentAmount,
                feePerKb = feePerKb
            )

            // No change = 1 output (Exact amount)
            if (paymentAmountNoChange.compareTo(collectedAmount) == 0) {
                assumeChange = false
                break
            }

            // No change = 1 output (Don't allow dust to be sent back as change - consume it rather)
            if (paymentAmountNoChange < collectedAmount &&
                paymentAmountNoChange >= collectedAmount.subtract(Payment.DUST)
            ) {
                consumedAmount = consumedAmount.add(paymentAmountNoChange.subtract(collectedAmount))
                assumeChange = false
                break
            }

            // Expect change = 2 outputs
            if (collectedAmount >= paymentAmountWithChange) {
                // [multiple inputs, 2 outputs] - assume change
                assumeChange = true
                break
            }
        }

        val outputs = when (assumeChange) {
            true -> listOf(targetOutputType, changeOutputType)
            else -> listOf(targetOutputType)
        }

        val absoluteFee = calculateFee(
            inputs = spendWorthyList,
            outputs = outputs,
            feePerKb = feePerKb,
            includesReplayDust = includesReplayDust
        )

        val paymentBundle = SpendableUnspentOutputs()
        paymentBundle.spendableOutputs = spendWorthyList
        paymentBundle.absoluteFee = absoluteFee
        paymentBundle.consumedAmount = consumedAmount
        paymentBundle.isReplayProtected = !requiresReplayProtection
        return paymentBundle
    }

    private fun calculateFee(
        inputs: List<Utxo>,
        outputs: List<OutputType>,
        feePerKb: BigInteger,
        includesReplayDust: Boolean
    ): BigInteger {
        if (inputs.isEmpty()) {
            return BigInteger.ZERO
        }

        if (includesReplayDust) {
            // No non-replayable outputs in wallet - a dust input and output will be added to tx later
            log.info("Modifying tx size for fee calculation.")
            val txBytes = Fees.estimatedSize(inputs, outputs) + DUST_INPUT_TX_SIZE_ADAPT
            return Fees.calculateFee(txBytes, feePerKb)
        }

        return Fees.estimatedFee(inputs, outputs, feePerKb)
    }

    private fun estimateAmount(
        inputs: List<Utxo>,
        outputs: List<OutputType>,
        paymentAmount: BigInteger,
        feePerKb: BigInteger
    ): BigInteger {
        val fee = Fees.estimatedFee(inputs, outputs, feePerKb)
        return paymentAmount.add(fee)
    }

    private fun requiresReplayProtection(unspentOutputs: List<Utxo>): Boolean {
        return unspentOutputs.isNotEmpty() && unspentOutputs[0].isReplayable
    }

    /**
     * Sort unspent outputs by amount in descending order.
     */
    private class UnspentOutputAmountComparatorDesc : Comparator<Utxo> {

        override fun compare(o1: Utxo, o2: Utxo): Int {
            return o2.value.compareTo(o1.value)
        }
    }

    private class UnspentOutputAmountComparatorAsc : Comparator<Utxo> {

        override fun compare(o1: Utxo, o2: Utxo): Int {
            return o1.value.compareTo(o2.value)
        }
    }

    private fun feePerKbToFeePerByte(feePerKb: BigInteger): BigInteger {
        return BigDecimal(feePerKb)
            .divide(BigDecimal.valueOf(1000L), 0, RoundingMode.CEILING)
            .toBigIntegerExact()
    }
}