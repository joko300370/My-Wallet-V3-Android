package piuk.blockchain.android.coincore

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Maybe
import io.reactivex.Single
import timber.log.Timber

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

    fun toUrl(amount: CryptoValue = CryptoValue.zero(asset)) = address

    // Temp for passing the results of a scan through to old send. Not needed for new
    // send, so only BTC, BCH and XLM will use this and it'll be taken out once the last
    // 3 assets are moved to the sendflow framework. TODO
    @Deprecated(message = "Old send scan support")
    val scanUri: String?
}

// Stub address type - the address parser should check and populate this properly, once BTC is
// new send enabled. TODO
class BitpayInvoiceTarget(
    override val asset: CryptoCurrency,
    val invoiceUrl: String
) : CryptoAddress {
    override val label: String = ""
    override val address: String = ""
    override val scanUri = invoiceUrl
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
            coincore.allAssets.map { asset ->
                asset.parseAddress(address)
                    .doOnError { Timber.e("**** ERROR: $asset") }
                    .onErrorComplete()
            }
        ).toList().map { it.toSet() }

    override fun parse(address: String, ccy: CryptoCurrency): Maybe<ReceiveAddress> =
        coincore[ccy].parseAddress(address)
}
