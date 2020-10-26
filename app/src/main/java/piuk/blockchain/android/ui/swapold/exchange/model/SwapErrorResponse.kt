package piuk.blockchain.android.ui.swapold.exchange.model

import com.blockchain.serialization.JsonSerializable

data class SwapErrorResponse(val id: String, val code: Int, val description: String) : JsonSerializable {

    fun toErrorType(): SwapErrorType =
        when (code) {
            41 -> SwapErrorType.ORDER_BELOW_MIN_LIMIT
            43 -> SwapErrorType.ORDER_ABOVE_MAX_LIMIT
            45 -> SwapErrorType.DAILY_LIMIT_EXCEEDED
            46 -> SwapErrorType.WEEKLY_LIMIT_EXCEEDED
            38 -> SwapErrorType.ALBERT_EXECUTION_ERROR
            else -> SwapErrorType.UNKNOWN
        }
}