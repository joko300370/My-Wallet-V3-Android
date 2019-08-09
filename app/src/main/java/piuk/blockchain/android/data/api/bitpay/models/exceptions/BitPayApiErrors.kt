package piuk.blockchain.android.data.api.bitpay.models.exceptions

import android.annotation.SuppressLint
import com.squareup.moshi.Moshi
import retrofit2.Response

private data class BitPayErrorResponse(
    val error: String
)

class BitPayApiException private constructor(message: String) : Throwable(message) {

    private var _httpErrorCode: Int = -1
    private lateinit var _error: String

    companion object {

        @SuppressLint("SyntheticAccessor")
        fun fromResponseBody(response: Response<*>?): BitPayApiException {
            val moshi = Moshi.Builder().build()
            val adapter = moshi.adapter(BitPayErrorResponse::class.java)
            val bitpayErrorResponse = adapter.fromJson(response?.errorBody()!!.string())!!

            val httpErrorCode = response.code()
            val error = bitpayErrorResponse.error

            return BitPayApiException("$httpErrorCode: $error")
                .apply {
                    _httpErrorCode = httpErrorCode
                    _error = error
                }
        }
    }
}