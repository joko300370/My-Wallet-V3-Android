package piuk.blockchain.android.data.api.bitpay.models.exceptions

import io.reactivex.Single
import retrofit2.HttpException

internal fun <T> Single<T>.wrapErrorMessage(): Single<T> = this.onErrorResumeNext {
    when (it) {
        is HttpException -> Single.error(BitPayApiException.fromResponseBody(it.response()))
        else -> Single.error(it)
    }
}
