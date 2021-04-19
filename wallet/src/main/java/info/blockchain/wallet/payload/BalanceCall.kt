package info.blockchain.wallet.payload

import info.blockchain.api.BitcoinApi
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.exceptions.ServerConnectionException
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.data.legacyXpubAddresses
import info.blockchain.wallet.payload.data.segwitXpubAddresses
import info.blockchain.wallet.payload.model.Balance
import info.blockchain.wallet.payload.model.toBalanceMap
import java.math.BigInteger

class BalanceCall(
    private val bitcoinApi: BitcoinApi,
    private val cryptoCurrency: CryptoCurrency
) : BalanceQuery {

    override fun getBalancesFor(xpubs: List<XPubs>, legacyImported: List<String>): Map<String, BigInteger> =
        getBalanceOfAddresses(
            legacyAddresses = xpubs.legacyXpubAddresses() + legacyImported,
            segwitAddresses = xpubs.segwitXpubAddresses()
        ).execute()
            .let {
                if (!it.isSuccessful) {
                    throw ServerConnectionException(it.errorBody()?.string() ?: "Unknown, no error body")
                }
                it.body()?.toBalanceMap()?.finalBalanceMap() ?: throw Exception("No balances returned")
            }

    private fun getBalanceOfAddresses(legacyAddresses: List<String>, segwitAddresses: List<String>) =
        bitcoinApi.getBalance(
            cryptoCurrency.networkTicker.toLowerCase(),
            legacyAddresses,
            segwitAddresses,
            BitcoinApi.BalanceFilter.RemoveUnspendable
        )
}

private fun <K> Map<K, Balance>.finalBalanceMap() =
    map { (k, v) -> k to v.finalBalance }.toMap()
