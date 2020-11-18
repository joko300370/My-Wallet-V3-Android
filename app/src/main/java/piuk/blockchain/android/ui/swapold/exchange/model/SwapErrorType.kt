package piuk.blockchain.android.ui.swapold.exchange.model

enum class SwapErrorType {
    ORDER_BELOW_MIN_LIMIT,
    ORDER_ABOVE_MAX_LIMIT,
    DAILY_LIMIT_EXCEEDED,
    WEEKLY_LIMIT_EXCEEDED,
    ANNUAL_LIMIT_EXCEEDED,
    ALBERT_EXECUTION_ERROR,
    CONFIRMATION_ETH_PENDING,
    UNKNOWN
}