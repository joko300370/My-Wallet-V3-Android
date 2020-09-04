package piuk.blockchain.android.ui.transfer.send.activity.strategy

import info.blockchain.balance.CryptoValue
import piuk.blockchain.androidcoreui.utils.extensions.getBoundary
import piuk.blockchain.androidcoreui.utils.logging.LoggingEvent

fun paymentSentEvent(success: Boolean, amountSent: CryptoValue) =
    LoggingEvent("Payment Sent", mapOf(
        "Success" to success,
        "Amount" to amountSent.toBigDecimal()
            .getBoundary() + " " + amountSent.currency.networkTicker,
        "Currency" to amountSent.currency.networkTicker
    ))
