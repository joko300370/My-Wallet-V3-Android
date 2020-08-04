package piuk.blockchain.android.coincore

import info.blockchain.balance.CryptoCurrency
import io.reactivex.Maybe
import io.reactivex.Single

class AddressParseError(val error: Error) : Exception("Error Parsing address") {

    enum class Error {
        ETH_UNEXPECTED_CONTRACT_ADDRESS
    }
}

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
    fun parse(address: String): Single<Set<ReceiveAddress>>
    fun parse(address: String, ccy: CryptoCurrency): Maybe<ReceiveAddress>
}

class AddressFactoryImpl(
    private val coincore: Coincore
) : AddressFactory {

    /** Build the set of possible address for a given input string.
     * If the string is not a valid address fir any available tokens, then return
     * an empty set
     **/
    override fun parse(address: String): Single<Set<ReceiveAddress>> =
        Maybe.merge(
            coincore.allAssets.map { it.parseAddress(address).onErrorComplete() }
        ).toList().map { it.toSet() }

    override fun parse(address: String, ccy: CryptoCurrency): Maybe<ReceiveAddress> =
        coincore[ccy].parseAddress(address)
}
