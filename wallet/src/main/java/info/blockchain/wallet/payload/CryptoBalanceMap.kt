package info.blockchain.wallet.payload

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.data.allAddresses
import java.math.BigInteger

data class CryptoBalanceMap(
    private val cryptoCurrency: CryptoCurrency,
    private val xpubs: List<XPubs>,
    private val imported: List<String>,
    private val balances: Map<String, BigInteger>
) {
    val totalSpendable = CryptoValue(cryptoCurrency, (xpubs.allAddresses() + imported).sum(balances))
    val totalSpendableImported: CryptoValue
        get() {
            return CryptoValue(cryptoCurrency, imported.sum(balances))
        }

    fun subtractAmountFromAddress(address: String, cryptoValue: CryptoValue): CryptoBalanceMap {
        val value =
            balances[address] ?: throw Exception("No info for this address. updateAllBalances should be called first.")
        val newBalances = balances.toMutableMap()
            .apply {
                set(address, value - cryptoValue.toBigInteger())
            }
        return copy(balances = newBalances)
    }

    operator fun get(address: String) =
        CryptoValue(cryptoCurrency, balances[address] ?: BigInteger.ZERO)

    companion object {
        @JvmStatic
        fun zero(cryptoCurrency: CryptoCurrency) =
            CryptoBalanceMap(
                cryptoCurrency,
                emptyList(),
                emptyList(),
                emptyMap()
            )
    }
}

fun calculateCryptoBalanceMap(
    cryptoCurrency: CryptoCurrency,
    balanceQuery: BalanceQuery,
    xpubs: List<XPubs>,
    imported: List<String>
): CryptoBalanceMap {
    return CryptoBalanceMap(
        cryptoCurrency,
        xpubs,
        imported,
        balanceQuery.getBalancesForXPubs(xpubs, imported)
    )
}

private fun <T> Iterable<T>.sum(balances: Map<T, BigInteger>) =
    map { balances[it] ?: BigInteger.ZERO }
        .foldRight(BigInteger.ZERO, BigInteger::add)
