package info.blockchain.wallet.payment

import info.blockchain.wallet.payload.model.Utxo
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

internal object Coins {

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
        addReplayProtection: Boolean
    ): Pair<BigInteger, BigInteger> {

        var coinSortingMethod: CoinSortingMethod? = null

        if (addReplayProtection) {
            coinSortingMethod = ReplayProtection(placeholderDustInput)
        }

        val selection = CoinSelection(utxoList, feePerKbToFeePerByte(feePerKb))
                .selectAll(targetOutputType, coinSortingMethod)

        return Pair(selection.spendableBalance, selection.absoluteFee)
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
        addReplayProtection: Boolean
    ): SpendableUnspentOutputs {

        val coinSortingMethod: CoinSortingMethod = if (addReplayProtection) {
            ReplayProtection(placeholderDustInput)
        } else {
            DescentDraw
        }

        return CoinSelection(utxoList, feePerKbToFeePerByte(feePerKb))
            .select(paymentAmount, targetOutputType, changeOutputType, coinSortingMethod)
    }

    private fun feePerKbToFeePerByte(feePerKb: BigInteger): BigInteger {
        return BigDecimal(feePerKb)
            .divide(BigDecimal.valueOf(1000L), 0, RoundingMode.CEILING)
            .toBigIntegerExact()
    }
}