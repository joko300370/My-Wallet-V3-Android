package info.blockchain.wallet.ethereum

import info.blockchain.wallet.ethereum.data.Erc20AddressResponse
import info.blockchain.wallet.ethereum.data.EthAddressResponseMap
import info.blockchain.wallet.ethereum.data.EthLatestBlockNumber
import info.blockchain.wallet.ethereum.data.EthPushTxRequest
import info.blockchain.wallet.ethereum.data.EthTransaction
import info.blockchain.wallet.ethereum.data.EthTransactionsResponse
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal interface EthEndpoints {
    @GET(EthUrls.ACCOUNT + "/{address}")
    fun getEthAccount(@Path("address") address: String): Observable<EthAddressResponseMap>

    @GET(EthUrls.ACCOUNT + "/{address}" + EthUrls.IS_CONTRACT)
    fun getIfContract(@Path("address") address: String): Observable<HashMap<String, Boolean>>

    @POST(EthUrls.PUSH_TX)
    fun pushTx(@Body ethPushTxRequest: EthPushTxRequest): Observable<HashMap<String, String>>

    @Headers("Accept: application/json")
    @GET(EthUrls.V2_DATA + "/block/latest/number")
    fun latestBlockNumber(): Single<EthLatestBlockNumber>

    @Headers("Accept: application/json")
    @GET(EthUrls.V2_DATA_TRANSACTION + "/{hash}")
    fun getTransaction(@Path("hash") txHash: String): Observable<EthTransaction>

    @GET("${EthUrls.V2_DATA_ACCOUNT}/{address}/transactions")
    @Headers("Accept: application/json")
    fun getTransactions(
        @Path("address") address: String,
        @Query("size") size: Int = 50
    ): Single<EthTransactionsResponse>

    @GET(EthUrls.V2_DATA_ACCOUNT + "/{address}/token/{hash}/wallet")
    @Headers("Accept: application/json")
    fun getErc20Address(
        @Path("address") address: String,
        @Path("hash") hash: String
    ): Observable<Erc20AddressResponse>
}