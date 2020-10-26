package piuk.blockchain.android.coincore.impl

import com.blockchain.swap.nabu.datamanagers.PriceTier
import java.math.BigDecimal
import java.math.RoundingMode

class PricesInterpolator(
    private val interpolator: Interpolator = LinearInterpolator(),
    list: List<PriceTier>
) {
    private val prices: List<PriceTier>

    init {
        prices = list.toMutableList().apply {
            this.add(0, PriceTier(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
        }.toList()
    }

    fun getRate(amount: BigDecimal): BigDecimal {
        prices.forEachIndexed { index, priceTier ->
            if (index == prices.size - 1) return priceTier.price

            val nextTier = prices[index + 1]
            val thisVol = priceTier.volume
            val nextVol = nextTier.volume

            if (thisVol < amount && amount <= nextVol) {
                return interpolator.interpolate(
                    listOf(priceTier.volume, nextTier.volume),
                    listOf(priceTier.price, nextTier.price), amount)
            }
        }
        return BigDecimal.ZERO
    }
}

class LinearInterpolator : Interpolator {

    override fun interpolate(x: List<BigDecimal>, y: List<BigDecimal>, xi: BigDecimal): BigDecimal {
        require(x.size == y.size) { "Should be same size" }
        require(x.size == 2) { "Should contain two points" }
        require(x.zipWithNext().all { it.first <= it.second }) { "$x Should be sorted" }
        require(xi >= x[0] && xi <= x[1]) { "$xi Should be between ${x[0]} and ${x[1]}" }
        return (((xi - x[0]) * (y[1] - y[0])).divide(x[1] - x[0], RoundingMode.HALF_UP)) + y[0]
        // Formula：Y = ( ( X - X1 )( Y2 - Y1) / ( X2 - X1) ) + Y1
        // X1, Y1 = first value, X2, Y2 = second value, X = target value, Y = result
    }
}

interface Interpolator {
    fun interpolate(x: List<BigDecimal>, y: List<BigDecimal>, xi: BigDecimal): BigDecimal
}