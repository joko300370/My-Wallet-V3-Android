package com.blockchain.swap.nabu.service

import com.blockchain.swap.nabu.api.nabu.Nabu
import com.blockchain.swap.nabu.datamanagers.SimpleBuyError
import com.blockchain.swap.nabu.extensions.wrapErrorMessage
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
import com.blockchain.swap.nabu.models.nabu.Scope
import com.blockchain.swap.nabu.models.nabu.SendToMercuryAddressRequest
import com.blockchain.swap.nabu.models.nabu.SendToMercuryAddressResponse
import com.blockchain.swap.nabu.models.nabu.SendWithdrawalAddressesRequest
import com.blockchain.swap.nabu.models.nabu.SupportedDocuments
import com.blockchain.swap.nabu.models.nabu.WalletMercuryLink
import com.blockchain.swap.nabu.models.simplebuy.BankAccountResponse
import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyBalanceResponse
import com.blockchain.swap.nabu.models.simplebuy.CustodialWalletOrder
import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyCurrency
import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyEligibility
import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyQuoteResponse
import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyPairsResp
import com.blockchain.swap.nabu.models.simplebuy.TransferRequest
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenRequest
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.swap.nabu.models.tokenresponse.NabuSessionTokenResponse
import com.blockchain.veriff.VeriffApplicantAndToken
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import retrofit2.HttpException
import retrofit2.Retrofit

class NabuService(retrofit: Retrofit) {

    private val service: Nabu = retrofit.create(Nabu::class.java)

    internal fun getAuthToken(
        jwt: String,
        currency: String? = null,
        action: String? = null
    ): Single<NabuOfflineTokenResponse> = service.getAuthToken(
        jwt = NabuOfflineTokenRequest(jwt), currency = currency, action = action
    ).wrapErrorMessage()

    internal fun getSessionToken(
        userId: String,
        offlineToken: String,
        guid: String,
        email: String,
        appVersion: String,
        deviceId: String
    ): Single<NabuSessionTokenResponse> = service.getSessionToken(
        userId,
        offlineToken,
        guid,
        email,
        appVersion,
        CLIENT_TYPE,
        deviceId
    ).wrapErrorMessage()

    internal fun createBasicUser(
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        sessionToken: NabuSessionTokenResponse
    ): Completable = service.createBasicUser(
        NabuBasicUser(firstName, lastName, dateOfBirth),
        sessionToken.authHeader
    )

    internal fun getUser(
        sessionToken: NabuSessionTokenResponse
    ): Single<NabuUser> = service.getUser(
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun getAirdropCampaignStatus(
        sessionToken: NabuSessionTokenResponse
    ): Single<AirdropStatusList> = service.getAirdropCampaignStatus(
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun updateWalletInformation(
        sessionToken: NabuSessionTokenResponse,
        jwt: String
    ): Single<NabuUser> = service.updateWalletInformation(
        NabuJwt(jwt),
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun getCountriesList(
        scope: Scope
    ): Single<List<NabuCountryResponse>> = service.getCountriesList(
        scope.value
    ).wrapErrorMessage()

    internal fun getStatesList(
        countryCode: String,
        scope: Scope
    ): Single<List<NabuStateResponse>> = service.getStatesList(
        countryCode,
        scope.value
    ).wrapErrorMessage()

    internal fun getSupportedDocuments(
        sessionToken: NabuSessionTokenResponse,
        countryCode: String
    ): Single<List<SupportedDocuments>> = service.getSupportedDocuments(
        countryCode,
        sessionToken.authHeader
    ).wrapErrorMessage()
        .map { it.documentTypes }

    internal fun addAddress(
        sessionToken: NabuSessionTokenResponse,
        line1: String,
        line2: String?,
        city: String,
        state: String?,
        postCode: String,
        countryCode: String
    ): Completable = service.addAddress(
        AddAddressRequest.fromAddressDetails(
            line1,
            line2,
            city,
            state,
            postCode,
            countryCode
        ),
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun recordCountrySelection(
        sessionToken: NabuSessionTokenResponse,
        jwt: String,
        countryCode: String,
        stateCode: String?,
        notifyWhenAvailable: Boolean
    ): Completable = service.recordSelectedCountry(
        RecordCountryRequest(
            jwt,
            countryCode,
            notifyWhenAvailable,
            stateCode
        ),
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun startVeriffSession(
        sessionToken: NabuSessionTokenResponse
    ): Single<VeriffApplicantAndToken> = service.startVeriffSession(
        sessionToken.authHeader
    ).map { VeriffApplicantAndToken(it.applicantId, it.token) }
        .wrapErrorMessage()

    internal fun submitVeriffVerification(
        sessionToken: NabuSessionTokenResponse
    ): Completable = service.submitVerification(
        ApplicantIdRequest(sessionToken.userId),
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun recoverUser(
        offlineToken: NabuOfflineTokenResponse,
        jwt: String
    ): Completable = service.recoverUser(
        offlineToken.userId,
        NabuJwt(jwt),
        authorization = "Bearer ${offlineToken.token}"
    ).wrapErrorMessage()

    internal fun registerCampaign(
        sessionToken: NabuSessionTokenResponse,
        campaignRequest: RegisterCampaignRequest,
        campaignName: String
    ): Completable = service.registerCampaign(
        campaignRequest,
        campaignName,
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun linkWalletWithMercury(
        sessionToken: NabuSessionTokenResponse
    ): Single<String> = service.connectWalletWithMercury(
        sessionToken.authHeader
    ).map { it.linkId }
        .wrapErrorMessage()

    internal fun linkMercuryWithWallet(
        sessionToken: NabuSessionTokenResponse,
        linkId: String
    ): Completable = service.connectMercuryWithWallet(
        sessionToken.authHeader,
        WalletMercuryLink(linkId)
    ).wrapErrorMessage()

    internal fun sendWalletAddressesToThePit(
        sessionToken: NabuSessionTokenResponse,
        request: SendWithdrawalAddressesRequest
    ): Completable = service.sharePitReceiveAddresses(
        sessionToken.authHeader,
        request
    ).wrapErrorMessage()

    internal fun fetchPitSendToAddressForCrypto(
        sessionToken: NabuSessionTokenResponse,
        cryptoSymbol: String
    ): Single<SendToMercuryAddressResponse> = service.fetchPitSendAddress(
        sessionToken.authHeader,
        SendToMercuryAddressRequest(cryptoSymbol)
    ).wrapErrorMessage()

    internal fun getSupportCurrencies(): Single<SimpleBuyPairsResp> = service.getSupportedSimpleBuyPairs()
        .wrapErrorMessage()

    fun getSimpleBuyBankAccountDetails(
        sessionToken: NabuSessionTokenResponse,
        currency: String
    ): Single<BankAccountResponse> =
        service.getSimpleBuyBankAccountDetails(
            sessionToken.authHeader, SimpleBuyCurrency(currency)
        ).wrapErrorMessage()

    internal fun getSimpleBuyQuote(
        sessionToken: NabuSessionTokenResponse,
        action: String,
        currencyPair: String,
        amount: String
    ): Single<SimpleBuyQuoteResponse> = service.getSimpleBuyQuote(
        authorization = sessionToken.authHeader,
        action = action,
        currencyPair = currencyPair,
        amount = amount
    )

    internal fun getPredefinedAmounts(
        sessionToken: NabuSessionTokenResponse,
        currency: String
    ): Single<List<Map<String, List<Long>>>> = service.getPredefinedAmounts(
        sessionToken.authHeader,
        currency
    ).wrapErrorMessage()

    internal fun isEligibleForSimpleBuy(
        sessionToken: NabuSessionTokenResponse
    ): Single<SimpleBuyEligibility> = service.isEligibleForSimpleBuy(
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun createOrder(
        sessionToken: NabuSessionTokenResponse,
        order: CustodialWalletOrder
    ) = service.createOrder(
        sessionToken.authHeader, order
    ).onErrorResumeNext {
        if (it is HttpException && it.code() == 409) {
            Single.error(SimpleBuyError.OrderLimitReached)
        } else {
            Single.error(it)
        }
    }.wrapErrorMessage()

    internal fun getOutstandingBuyOrders(
        sessionToken: NabuSessionTokenResponse
    ) = service.getOutstandingBuyOrders(
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun deleteBuyOrder(
        sessionToken: NabuSessionTokenResponse,
        orderId: String
    ) = service.deleteBuyOrder(
        sessionToken.authHeader, orderId
    ).onErrorResumeNext {
        if (it is HttpException && it.code() == 409) {
            Completable.error(SimpleBuyError.OrderNotCancelable)
        } else {
            Completable.error(it)
        }
    }.wrapErrorMessage()

    fun getBuyOrder(
        sessionToken: NabuSessionTokenResponse,
        orderId: String
    ) = service.getBuyOrder(
        sessionToken.authHeader, orderId
    ).wrapErrorMessage()

    @Suppress("MoveLambdaOutsideParentheses")
    fun getBalanceForAsset(
        sessionToken: NabuSessionTokenResponse,
        cryptoCurrency: CryptoCurrency
    ): Maybe<SimpleBuyBalanceResponse> = service.getBalanceForAsset(
        sessionToken.authHeader, cryptoCurrency.symbol
    ).flatMapMaybe {
        when (it.code()) {
            200 -> Maybe.just(it.body())
            204 -> Maybe.empty()
            else -> Maybe.error(HttpException(it))
        }
    }

    fun transferFunds(
        sessionToken: NabuSessionTokenResponse,
        request: TransferRequest
    ): Completable = service.transferFunds(
        sessionToken.authHeader,
        request
    ).onErrorResumeNext {
        if (it is HttpException) {
            when (it.code()) {
                403 -> Completable.error(SimpleBuyError.WithdrawlAlreadyPending)
                409 -> Completable.error(SimpleBuyError.WithdrawlInsufficientFunds)
                else -> Completable.error(it)
            }
        } else {
            Completable.error(it)
        }
    }.wrapErrorMessage()

    companion object {
        internal const val CLIENT_TYPE = "APP"
    }
}
