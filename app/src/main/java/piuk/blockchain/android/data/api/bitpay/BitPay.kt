package piuk.blockchain.android.data.api.bitpay

import piuk.blockchain.android.data.api.bitpay.models.RawPaymentRequest
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

interface BitPay {

    @GET
    fun getRawPaymentRequest(
        @Url path: String,
        @Header("Accept") acceptType: String
    ): Single<RawPaymentRequest>
}
