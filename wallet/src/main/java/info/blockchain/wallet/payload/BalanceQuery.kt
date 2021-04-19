package info.blockchain.wallet.payload

import info.blockchain.wallet.payload.data.XPubs
import java.math.BigInteger

interface BalanceQuery {
    fun getBalancesFor(
        xpubs: List<XPubs>,
        legacyImported: List<String> = emptyList()
    ): Map<String, BigInteger>
}