package info.blockchain.wallet.ethereum

import info.blockchain.wallet.ApiCode
import info.blockchain.wallet.BlockchainFramework
import info.blockchain.wallet.ethereum.data.Erc20AddressResponse
import info.blockchain.wallet.ethereum.data.EthAddressResponse
import info.blockchain.wallet.ethereum.data.EthAddressResponseMap
import info.blockchain.wallet.ethereum.data.EthLatestBlock
import info.blockchain.wallet.ethereum.data.EthLatestBlockNumber
import info.blockchain.wallet.ethereum.data.EthPushTxRequest
import info.blockchain.wallet.ethereum.data.EthTransaction
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single

class EthAccountApi(private val apiCode: ApiCode) {

    private var endpoints: EthEndpoints? = null

    /**
     * Returns information about the latest block via a [EthLatestBlock] object.
     *
     * @return An [Single] wrapping an [EthLatestBlock]
     */
    val latestBlockNumber: Single<EthLatestBlockNumber>
        get() = apiInstance.latestBlockNumber()

    /**
     * Lazily evaluates an instance of [EthEndpoints].
     */
    private val apiInstance: EthEndpoints
        get() = endpoints ?: BlockchainFramework.getRetrofitApiInstance().create(EthEndpoints::class.java)

    /**
     * Returns an [EthAddressResponse] object for a list of given ETH addresses as an [ ].
     * An [EthAddressResponse] contains a list of transactions associated with
     * the accounts, as well as a final balance for each.
     *
     * @param addresses The ETH addresses to be queried
     * @return An [Observable] wrapping an [EthAddressResponse]
     */
    fun getEthAddress(addresses: List<String>): Observable<EthAddressResponseMap> {
        return apiInstance.getEthAccount(addresses.joinToString(","))
    }

    fun getEthTransactions(addresses: List<String>): Single<List<EthTransaction>> {
        return apiInstance.getTransactions(addresses.joinToString(",")).map { it.transactions }
    }

    fun getLastEthTransaction(addresses: List<String>): Maybe<EthTransaction> {
        return apiInstance.getTransactions(addresses.joinToString(","), 1)
            .flatMapMaybe {
                if (it.transactions.isNotEmpty())
                    Maybe.just(it.transactions[0])
                else Maybe.empty()
            }
        }

    /**
     * Returns true if a given ETH address is associated with an Ethereum contract, which is
     * currently unsupported. This should be used to validate any proposed destination address for
     * funds.
     *
     * @param address The ETH address to be queried
     * @return An [Observable] returning true or false based on the address's contract status
     */
    fun getIfContract(address: String): Observable<Boolean> {
        return apiInstance.getIfContract(address)
            .map { map -> map["contract"] }
    }

    /**
     * Executes signed eth transaction and returns transaction hash.
     *
     * @param rawTx The ETH address to be queried
     * @return An [Observable] returning the transaction hash of a completed transaction.
     */
    fun pushTx(rawTx: String): Observable<String> {
        val request = EthPushTxRequest(rawTx, apiCode.apiCode)
        return apiInstance.pushTx(request)
            .map { map -> map["txHash"]!! }
    }

    fun getErc20Address(address: String, hash: String): Observable<Erc20AddressResponse> {
        return apiInstance.getErc20Address(address, hash)
    }

    /**
     * Returns an [EthTransaction] containing information about a specific ETH transaction.
     *
     * @param hash The hash of the transaction you wish to check
     * @return An [Observable] wrapping an [EthTransaction]
     */
    fun getTransaction(hash: String): Observable<EthTransaction> {
        return apiInstance.getTransaction(hash)
    }
}
