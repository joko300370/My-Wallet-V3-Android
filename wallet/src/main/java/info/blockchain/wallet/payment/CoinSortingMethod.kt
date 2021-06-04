package info.blockchain.wallet.payment

import info.blockchain.wallet.payload.model.Utxo

/**
 * Sort coins for different selection optimizations.
 */
interface CoinSortingMethod {
    fun sort(coins: List<Utxo>): List<Utxo>
}

/**
 * Prioritizes smaller coins, better coin consolidation but a higher fee.
 */
object AscentDraw : CoinSortingMethod {
    override fun sort(coins: List<Utxo>) = coins.sortedBy { it.value }
}

/**
 * Prioritizes larger coins, worse coin consolidation but a lower fee.
 */
object DescentDraw : CoinSortingMethod {
    override fun sort(coins: List<Utxo>) = coins.sortedByDescending { it.value }
}

/**
 * The smallest non-replayable coin, followed by all replayable coins (largest to smallest),
 * followed by all remaining non-replayable coins (also largest to smallest). Adds replay protection.
 */
class ReplayProtection(private val nonReplayableInput: Utxo) : CoinSortingMethod {
    override fun sort(coins: List<Utxo>): List<Utxo> {
        if (coins.isEmpty()) {
            return coins
        }
        val (replayable, nonReplayable) = AscentDraw.sort(coins).partition {
            it.isReplayable
        }
        return listOf(nonReplayable.firstOrNull() ?: nonReplayableInput) +
                DescentDraw.sort(replayable) +
                DescentDraw.sort(nonReplayable.drop(1))
    }
}
