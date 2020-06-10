package piuk.blockchain.android.coincore

import info.blockchain.balance.CryptoCurrency

interface ReceiveAddress {
    val label: String
}

interface CryptoAddress : ReceiveAddress {
    val asset: CryptoCurrency
    val address: String
}

typealias AddressList = List<ReceiveAddress>

object NullAddress : CryptoAddress {
    override val asset: CryptoCurrency = CryptoCurrency.BTC
    override val address: String = ""
    override val label: String = ""
}

interface AddressFactory {
    fun parse(address: String): Set<CryptoAddress>
    fun parse(address: String, ccy: CryptoCurrency): CryptoAddress?
}

class AddressFactoryImpl(
    private val coincore: Coincore
) : AddressFactory {

    /** Build the set of possible address for a given input string.
     * If the string is not a valid address fir any available tokens, then return
     * an empty set
     **/
    override fun parse(address: String): Set<CryptoAddress> =
        coincore.tokens.mapNotNull { t: AssetTokens ->
            t.parseAddress(address)
        }.toSet()

    override fun parse(address: String, ccy: CryptoCurrency): CryptoAddress? =
        coincore[ccy].parseAddress(address)
}
