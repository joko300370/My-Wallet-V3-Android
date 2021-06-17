package info.blockchain.wallet.exceptions

import com.blockchain.api.ApiException
import okhttp3.ResponseBody

class TransactionHashApiException(
    message: String,
    val hashString: String
) : ApiException(message) {

    companion object {
        fun fromResponse(hashString: String, response: retrofit2.Response<ResponseBody>): Throwable {
            return TransactionHashApiException("${response.code()}: ${response.errorBody()!!.string()}", hashString)
        }
    }
}
