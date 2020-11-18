package piuk.blockchain.android.ui.swapold.logging

import piuk.blockchain.androidcoreui.utils.logging.LoggingEvent

fun fixTypeEvent(fixType: FixType) =
    LoggingEvent("Fix type switched", mapOf("Input Type" to fixType.name))

enum class FixType(val type: String) {
    BaseFiat("Base fiat"),
    BaseCrypto("Base crypto"),
    CounterFiat("Counter fiat"),
    CounterCrypto("Counter crypto")
}

fun websocketConnectionFailureEvent() =
    LoggingEvent("Websocket connection failure", mapOf("Websocket connection failed" to true))

fun amountErrorEvent(errorType: AmountErrorType) =
    LoggingEvent("Min/Max error", mapOf("Min/Max error type" to errorType.error))

enum class AmountErrorType(val error: String) {
    OverBalance("Over user's balance"),
    OverMax("Over max"),
    UnderMin("Under min")
}