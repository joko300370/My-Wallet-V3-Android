package piuk.blockchain.android.data.api.bitpay

import io.reactivex.Single
import piuk.blockchain.android.data.api.bitpay.models.RawPaymentRequest
import piuk.blockchain.android.data.api.bitpay.models.exceptions.wrapErrorMessage
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.rxjava.RxPinning
import retrofit2.Retrofit
import javax.inject.Named

class BitPayService constructor(
    environmentConfig: EnvironmentConfig,
    @Named("kotlin") retrofit: Retrofit,
    rxBus: RxBus
) {

    private val service: BitPay = retrofit.create(BitPay::class.java)
    private val rxPinning: RxPinning = RxPinning(rxBus)
    private val baseUrl: String = environmentConfig.bitpayUrl

    internal fun getRawPaymentRequest(
        path: String = "$baseUrl$PATH_BITPAY_INVOICE",
        invoiceId: String
    ): Single<RawPaymentRequest> = rxPinning.callSingle {
        service.getRawPaymentRequest("$path/$invoiceId", "application/payment-request")
            .wrapErrorMessage()
    }
}