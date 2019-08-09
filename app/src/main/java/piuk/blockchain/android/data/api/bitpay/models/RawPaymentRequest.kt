package piuk.blockchain.android.data.api.bitpay.models

import java.math.BigInteger

data class RawPaymentRequest(
    val outputs: List<BitPayPaymentRequestOutput>,
    val memo: String,
    val expires: String,
    val paymentUrl: String
)

data class BitPayPaymentRequestOutput(
    val amount: BigInteger,
    val address: String
)