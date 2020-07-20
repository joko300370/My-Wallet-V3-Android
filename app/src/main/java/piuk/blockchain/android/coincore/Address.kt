package piuk.blockchain.android.coincore

import info.blockchain.balance.CryptoCurrency

interface SendTarget {
    val label: String
}

interface ReceiveAddress : SendTarget

object NullAddress : ReceiveAddress {
    override val label: String = ""
}

interface CryptoAddress : ReceiveAddress {
    val asset: CryptoCurrency
    val address: String
}

interface AddressFactory {
    fun parse(address: String): Set<ReceiveAddress>
    fun parse(address: String, ccy: CryptoCurrency): ReceiveAddress?
}

class AddressFactoryImpl(
    private val coincore: Coincore
) : AddressFactory {

    /** Build the set of possible address for a given input string.
     * If the string is not a valid address fir any available tokens, then return
     * an empty set
     **/

    override fun parse(address: String): Set<ReceiveAddress> =
        coincore.allAssets.mapNotNull { t: Asset ->
            t.parseAddress(address)
        }.toSet()

    override fun parse(address: String, ccy: CryptoCurrency): ReceiveAddress? =
        coincore[ccy].parseAddress(address)
}
