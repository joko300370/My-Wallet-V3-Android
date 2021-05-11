package info.blockchain.wallet.payload

import info.blockchain.wallet.payload.data.XPubs
import java.math.BigInteger

interface BalanceQuery {
    fun getBalancesForXPubs(
        xpubs: List<XPubs>,
        legacyImported: List<String>
    ): Map<String, BigInteger>

    fun getBalancesForAddresses(
        addresses: List<String>,
        legacyImported: List<String> = emptyList()
    ): Map<String, BigInteger>
}