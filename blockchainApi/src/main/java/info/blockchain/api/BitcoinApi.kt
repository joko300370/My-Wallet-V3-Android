package info.blockchain.api

import info.blockchain.api.bitcoin.data.MultiAddress
import info.blockchain.api.bitcoin.data.UnspentOutputsDto
import info.blockchain.api.bitcoin.BitcoinApiInterface
import info.blockchain.api.bitcoin.data.BalanceResponseDto
import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.HttpException
import retrofit2.Retrofit

class BitcoinApi(
    retrofitApiRoot: Retrofit,
    private val apiCode: String
) {
    private val bitcoinApiInterface: BitcoinApiInterface = retrofitApiRoot.create(BitcoinApiInterface::class.java)

    enum class BalanceFilter(val filterInt: Int) {
        All(4),
        ConfirmedOnly(5),
        RemoveUnspendable(6);
    }

    /**
     * Returns the address balance summary for each address provided
     *
     * @param coin The code of the coin to be returned, ie "btc" or "bch"
     * @param addressAndXpubListLegacy List of addresses and legacy xpubs.
     * All addresses should be passed through this parameter. In base58, bech32 or xpub format.
     * @param xpubListBech32 Segwit xpub addresses. Do not pass normal addresses here.
     * @param filter the filter for transactions selection, use null to indicate default
     * @return [Call] object which can be executed synchronously or asynchronously to return a
     * response object
     */
    @Deprecated("Use the Rx version")
    fun getBalance(
        coin: String,
        addressAndXpubListLegacy: List<String>,
        xpubListBech32: List<String>,
        filter: BalanceFilter
    ): Call<BalanceResponseDto> {
        val legacyAddrAndXpubs = addressAndXpubListLegacy.joinToString(",")
        val bech32Xpubs = xpubListBech32.joinToString(",")

        return bitcoinApiInterface.getBalance(
            coin,
            legacyAddrAndXpubs,
            bech32Xpubs,
            filter.filterInt,
            apiCode
        )
    }

    /**
     * Returns the address balance summary for each address provided
     *
     * @param coin The code of the coin to be returned, ie "btc" or "bch"
     * @param addressAndXpubListLegacy List of addresses and legacy xpubs.
     * All addresses should be passed through this parameter. In base58, bech32 or xpub format.
     * @param xpubListBech32 Segwit xpub addresses. Do not pass normal addresses here.
     * @param filter the filter for transactions selection, use null to indicate default
     */
    fun getBalanceRx(
        coin: String,
        addressAndXpubListLegacy: List<String>,
        xpubListBech32: List<String>,
        filter: BalanceFilter
    ): Single<BalanceResponseDto> {
        val legacyAddressesAndXpubs = addressAndXpubListLegacy.joinToString(",")
        val bech32Xpubs = xpubListBech32.joinToString("|")

        return bitcoinApiInterface.getBalanceRx(
            coin,
            legacyAddressesAndXpubs,
            bech32Xpubs,
            filter.filterInt,
            apiCode
        )
    }

    /**
     * Returns an aggregated summary on all addresses provided.
     *
     * @param coin The code of the coin to be returned, ie "btc" or "bch"
     * @param addressListLegacy a list of Base58 or xpub addresses
     * @param filter the filter for transactions selection, use null to indicate default
     * @param limit an integer to limit number of transactions to display, use null to indicate default
     * @param offset an integer to set number of transactions to skip when fetch
     * @param context A context for the results
     * @return [Call] object which can be executed synchronously or asynchronously to return a
     * response object
     */
    fun getMultiAddress(
        coin: String,
        addressListLegacy: List<String>,
        addressListBech32: List<String>,
        context: String?,
        filter: BalanceFilter,
        limit: Int,
        offset: Int
    ): Call<MultiAddress> {
        val legacyAddresses = addressListLegacy.joinToString("|")
        val bech32Addresses = addressListBech32.joinToString("|")

        return bitcoinApiInterface.getMultiAddress(
            coin,
            legacyAddresses,
            bech32Addresses,
            limit,
            offset,
            filter.filterInt,
            context,
            apiCode
        )
    }

    /**
     * Returns list of unspent outputs.
     *
     * @param coin The code of the coin to be returned, ie "btc" or "bch"
     * @param addressList a list of Base58 or xpub addresses
     * @param confirms an integer for minimum confirms of the outputs, use null to indicate default
     * @param limit an integer to limit number of transactions to display, use null to indicate default
     * @return [Call] object which can be executed synchronously or asynchronously to return a
     * response object
     */
    fun getUnspentOutputs(
        coin: String,
        addressListLegacy: List<String>,
        addressListBech32: List<String>,
        confirms: Int?,
        limit: Int?
    ): Single<UnspentOutputsDto> {
        val legacyAddresses = addressListLegacy.joinToString("|")
        val bech32Addresses = addressListBech32.joinToString("|")

        return bitcoinApiInterface.getUnspent(
            coin,
            legacyAddresses,
            bech32Addresses,
            confirms,
            limit,
            apiCode
        ).onErrorResumeNext { e ->
            when {
                e is HttpException && e.code() == 500 -> Single.just(UnspentOutputsDto())
                else -> Single.error(e) // TODO: Wrap in ApiAException
            }
        }
    }

    /**
     * Push a Bitcoin or Bitcoin Cash transaction to network.
     *
     * @param coin The coin type, either BTC or BCH
     * @param hash Transaction hash
     * @return [Call] object which can be executed synchronously or asynchronously to return a
     * response object
     */
    fun pushTx(coin: String, hash: String): Call<ResponseBody> {
        return bitcoinApiInterface.pushTx(coin, hash, apiCode)
    }

    /**
     * Push transaction to network with lock secret.
     *
     * @param coin The coin type, either BTC or BCH
     * @param hash Transaction hash
     * @param lockSecret Secret used server side
     * @return [Call] object which can be executed synchronously or asynchronously to return a
     * response object
     */
    fun pushTxWithSecret(
        coin: String,
        hash: String,
        lockSecret: String
    ): Call<ResponseBody> {
        return bitcoinApiInterface.pushTxWithSecret(coin, hash, lockSecret, apiCode)
    }

    companion object {
        const val BITCOIN = "btc"
        const val BITCOIN_CASH = "bch"
    }
}
