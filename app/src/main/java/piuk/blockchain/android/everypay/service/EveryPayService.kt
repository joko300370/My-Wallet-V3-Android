package piuk.blockchain.android.everypay.service

import io.reactivex.Single
import piuk.blockchain.android.everypay.models.CardDetailRequest
import piuk.blockchain.android.everypay.models.CardDetailResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface EveryPayService {

    @POST("api/v3/mobile_payments/card_details")
    fun getCardDetail(
        @Body cardDetailRequest: CardDetailRequest,
        @Header("Authorization") accessToken: String
    ): Single<CardDetailResponse>
}