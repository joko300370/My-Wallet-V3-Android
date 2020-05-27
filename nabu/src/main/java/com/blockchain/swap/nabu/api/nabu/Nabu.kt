package com.blockchain.swap.nabu.api.nabu

import com.blockchain.swap.nabu.models.cards.CardResponse
import com.blockchain.swap.nabu.models.cards.PaymentMethodsResponse
import com.blockchain.swap.nabu.models.interest.InterestAccountBalanceResponse
import com.blockchain.swap.nabu.models.interest.InterestResponse
import com.blockchain.swap.nabu.models.nabu.AddAddressRequest
import com.blockchain.swap.nabu.models.nabu.AirdropStatusList
import com.blockchain.swap.nabu.models.nabu.ApplicantIdRequest
import com.blockchain.swap.nabu.models.nabu.NabuBasicUser
import com.blockchain.swap.nabu.models.nabu.NabuCountryResponse
import com.blockchain.swap.nabu.models.nabu.NabuJwt
import com.blockchain.swap.nabu.models.nabu.NabuStateResponse
import com.blockchain.swap.nabu.models.nabu.NabuUser
import com.blockchain.swap.nabu.models.nabu.RecordCountryRequest
import com.blockchain.swap.nabu.models.nabu.RegisterCampaignRequest
import com.blockchain.swap.nabu.models.nabu.SendToMercuryAddressRequest
import com.blockchain.swap.nabu.models.nabu.SendToMercuryAddressResponse
import com.blockchain.swap.nabu.models.nabu.SendWithdrawalAddressesRequest
import com.blockchain.swap.nabu.models.nabu.SupportedDocumentsResponse
import com.blockchain.swap.nabu.models.nabu.TierUpdateJson
import com.blockchain.swap.nabu.models.nabu.TiersJson
import com.blockchain.swap.nabu.models.nabu.UpdateCoinifyTraderIdRequest
import com.blockchain.swap.nabu.models.nabu.VeriffToken
import com.blockchain.swap.nabu.models.nabu.WalletMercuryLink
import com.blockchain.swap.nabu.models.simplebuy.ActivateCardResponse
import com.blockchain.swap.nabu.models.simplebuy.AddNewCardBodyRequest
import com.blockchain.swap.nabu.models.simplebuy.AddNewCardResponse
import com.blockchain.swap.nabu.models.simplebuy.BankAccountResponse
import com.blockchain.swap.nabu.models.simplebuy.BuyOrderListResponse
import com.blockchain.swap.nabu.models.simplebuy.BuyOrderResponse
import com.blockchain.swap.nabu.models.simplebuy.CardPartnerAttributes
import com.blockchain.swap.nabu.models.simplebuy.ConfirmOrderRequestBody
import com.blockchain.swap.nabu.models.simplebuy.CustodialWalletOrder
import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyBalanceResponse
import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyCurrency
import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyEligibility
import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyPairsResp
import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyQuoteResponse
import com.blockchain.swap.nabu.models.simplebuy.TransferRequest
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenRequest
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.swap.nabu.models.tokenresponse.NabuSessionTokenResponse
import io.reactivex.Completable
import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

internal interface Nabu {

    @POST(NABU_INITIAL_AUTH)
    fun getAuthToken(
        @Body jwt: NabuOfflineTokenRequest,
        @Query("fiatCurrency") currency: String? = null,
        @Query("action") action: String? = null
    ): Single<NabuOfflineTokenResponse>

    @POST(NABU_SESSION_TOKEN)
    fun getSessionToken(
        @Query("userId") userId: String,
        @Header("authorization") authorization: String,
        @Header("X-WALLET-GUID") guid: String,
        @Header("X-WALLET-EMAIL") email: String,
        @Header("X-APP-VERSION") appVersion: String,
        @Header("X-CLIENT-TYPE") clientType: String,
        @Header("X-DEVICE-ID") deviceId: String
    ): Single<NabuSessionTokenResponse>

    @PUT(NABU_USERS_CURRENT)
    fun createBasicUser(
        @Body basicUser: NabuBasicUser,
        @Header("authorization") authorization: String
    ): Completable

    @GET(NABU_USERS_CURRENT)
    fun getUser(
        @Header("authorization") authorization: String
    ): Single<NabuUser>

    @GET(NABU_AIRDROP_CENTRE)
    fun getAirdropCampaignStatus(
        @Header("authorization") authorization: String
    ): Single<AirdropStatusList>

    @PUT(NABU_UPDATE_WALLET_INFO)
    fun updateWalletInformation(
        @Body jwt: NabuJwt,
        @Header("authorization") authorization: String
    ): Single<NabuUser>

    @GET(NABU_COUNTRIES)
    fun getCountriesList(
        @Query("scope") scope: String?
    ): Single<List<NabuCountryResponse>>

    @GET("$NABU_COUNTRIES/{regionCode}/$NABU_STATES")
    fun getStatesList(
        @Path("regionCode") countryCode: String,
        @Query("scope") scope: String?
    ): Single<List<NabuStateResponse>>

    @GET("$NABU_SUPPORTED_DOCUMENTS/{countryCode}")
    fun getSupportedDocuments(
        @Path("countryCode") countryCode: String,
        @Header("authorization") authorization: String
    ): Single<SupportedDocumentsResponse>

    @PUT(NABU_PUT_ADDRESS)
    fun addAddress(
        @Body address: AddAddressRequest,
        @Header("authorization") authorization: String
    ): Completable

    @POST(NABU_RECORD_COUNTRY)
    fun recordSelectedCountry(
        @Body recordCountryRequest: RecordCountryRequest,
        @Header("authorization") authorization: String
    ): Completable

    /**
     * This is a GET, but it actually starts a veriff session on the server for historical reasons.
     * So do not call more than once per veriff launch.
     */

    @GET(NABU_VERIFF_TOKEN)
    fun startVeriffSession(
        @Header("authorization") authorization: String
    ): Single<VeriffToken>

    @POST(NABU_SUBMIT_VERIFICATION)
    fun submitVerification(
        @Body applicantIdRequest: ApplicantIdRequest,
        @Header("authorization") authorization: String
    ): Completable

    @POST("$NABU_RECOVER_USER/{userId}")
    fun recoverUser(
        @Path("userId") userId: String,
        @Body jwt: NabuJwt,
        @Header("authorization") authorization: String
    ): Completable

    @PUT(NABU_REGISTER_CAMPAIGN)
    fun registerCampaign(
        @Body campaignRequest: RegisterCampaignRequest,
        @Header("X-CAMPAIGN") campaignHeader: String,
        @Header("authorization") authorization: String
    ): Completable

    @GET(NABU_KYC_TIERS)
    fun getTiers(
        @Header("authorization") authorization: String
    ): Single<TiersJson>

    @POST(NABU_KYC_TIERS)
    fun setTier(
        @Body tierUpdateJson: TierUpdateJson,
        @Header("authorization") authorization: String
    ): Completable

    @PUT(NABU_UPDATE_COINIFY_ID)
    fun setCoinifyTraderId(
        @Body coinifyTraderId: UpdateCoinifyTraderIdRequest,
        @Header("authorization") authorization: String
    ): Completable

    @PUT(NABU_CONNECT_WALLET_TO_PIT)
    fun connectWalletWithMercury(
        @Header("authorization") authorization: String
    ): Single<WalletMercuryLink>

    @PUT(NABU_CONNECT_PIT_TO_WALLET)
    fun connectMercuryWithWallet(
        @Header("authorization") authorization: String,
        @Body linkId: WalletMercuryLink
    ): Completable

    @POST(NABU_SEND_WALLET_ADDRESSES_TO_PIT)
    fun sharePitReceiveAddresses(
        @Header("authorization") authorization: String,
        @Body addresses: SendWithdrawalAddressesRequest
    ): Completable

    @PUT(NABU_FETCH_PIT_ADDRESS_FOR_WALLET)
    fun fetchPitSendAddress(
        @Header("authorization") authorization: String,
        @Body currency: SendToMercuryAddressRequest
    ): Single<SendToMercuryAddressResponse>

    @GET(NABU_SIMPLE_BUY_PAIRS)
    fun getSupportedSimpleBuyPairs(
        @Query("fiatCurrency") fiatCurrency: String? = null
    ): Single<SimpleBuyPairsResp>

    @GET(NABU_SIMPLE_BUY_AMOUNTS)
    fun getPredefinedAmounts(
        @Header("authorization") authorization: String,
        @Query("currency") currency: String
    ): Single<List<Map<String, List<Long>>>>

    @GET(NABU_SIMPLE_QUOTE)
    fun getSimpleBuyQuote(
        @Header("authorization") authorization: String,
        @Query("currencyPair") currencyPair: String,
        @Query("action") action: String,
        @Query("amount") amount: String
    ): Single<SimpleBuyQuoteResponse>

    @PUT(NABU_SIMPLE_BUY_ACCOUNT_DETAILS)
    fun getSimpleBuyBankAccountDetails(
        @Header("authorization") authorization: String,
        @Body currency: SimpleBuyCurrency
    ): Single<BankAccountResponse>

    @GET(NABU_SIMPLE_BUY_ELIGIBILITY)
    fun isEligibleForSimpleBuy(
        @Header("authorization") authorization: String,
        @Query("fiatCurrency") fiatCurrency: String,
        @Query("methods") methods: String
    ): Single<SimpleBuyEligibility>

    @POST(NABU_SIMPLE_BUY_ORDERS)
    fun createOrder(
        @Header("authorization") authorization: String,
        @Query("action") action: String?,
        @Body order: CustodialWalletOrder
    ): Single<BuyOrderResponse>

    @GET(NABU_SIMPLE_BUY_ORDERS)
    fun getBuyOrders(
        @Header("authorization") authorization: String,
        @Query("pendingOnly") pendingOnly: Boolean
    ): Single<BuyOrderListResponse>

    @DELETE("$NABU_SIMPLE_BUY_ORDERS/{orderId}")
    fun deleteBuyOrder(
        @Header("authorization") authorization: String,
        @Path("orderId") orderId: String
    ): Completable

    @GET("$NABU_SIMPLE_BUY_ORDERS/{orderId}")
    fun getBuyOrder(
        @Header("authorization") authHeader: String,
        @Path("orderId") orderId: String
    ): Single<BuyOrderResponse>

    @POST("$NABU_SIMPLE_BUY_ORDERS/{orderId}")
    fun confirmOrder(
        @Header("authorization") authHeader: String,
        @Path("orderId") orderId: String,
        @Body confirmBody: ConfirmOrderRequestBody
    ): Single<BuyOrderResponse>

    @POST(NABU_CARDS)
    fun addNewCard(
        @Header("authorization") authHeader: String,
        @Body addNewCardBody: AddNewCardBodyRequest
    ): Single<AddNewCardResponse>

    @DELETE("$NABU_CARDS/{cardId}")
    fun deleteCard(
        @Header("authorization") authHeader: String,
        @Path("cardId") cardId: String
    ): Completable

    @POST("$NABU_CARDS/{cardId}/activate")
    fun activateCard(
        @Header("authorization") authHeader: String,
        @Path("cardId") cardId: String,
        @Body attributes: CardPartnerAttributes
    ): Single<ActivateCardResponse>

    @GET("$NABU_CARDS/{cardId}")
    fun getCardDetails(
        @Header("authorization") authorization: String,
        @Path("cardId") cardId: String
    ): Single<CardResponse>

    @GET(NABU_PAYMENT_METHODS)
    fun getPaymentMethods(
        @Header("authorization") authorization: String,
        @Query("currency") currency: String,
        @Query("checkEligibility") checkEligibility: Boolean
    ): Single<PaymentMethodsResponse>

    @GET(NABU_CARDS)
    fun getCards(
        @Header("authorization") authorization: String
    ): Single<List<CardResponse>>

    @GET(NABU_SIMPLE_BUY_ASSET_BALANCE)
    fun getBalanceForAsset(
        @Header("authorization") authorization: String,
        @Query("ccy") cryptoSymbol: String
    ): Single<Response<SimpleBuyBalanceResponse>>

    @Headers("blockchain-origin: simplebuy")
    @POST(NABU_SIMPLE_BUY_BALANCE_TRANSFER)
    fun transferFunds(
        @Header("authorization") authorization: String,
        @Body request: TransferRequest
    ): Completable

    @GET(NABU_INTEREST_RATES)
    fun getInterestRates(
        @Header("authorization") authorization: String,
        @Query("ccy") currency: String
    ): Single<Response<InterestResponse>>

    @GET(NABU_INTEREST_ACCOUNT_BALANCE)
    fun getInterestAccountBalance(
        @Header("authorization") authorization: String,
        @Query("ccy") cryptoSymbol: String
    ): Single<Response<InterestAccountBalanceResponse>>
}
