package piuk.blockchain.android.ui.transactionflow.analytics

import com.blockchain.notifications.analytics.AnalyticsEvent

enum class WithdrawAnalytics(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {
    WITHDRAW_SHOWN("cash_withdraw_form_shown"),
    WITHDRAW_CONFIRM("cash_withdraw_form_confirm_click"),
    WITHDRAW_CHECKOUT_SHOWN("cash_withdraw_checkout_shown"),
    WITHDRAW_CHECKOUT_CONFIRM("cash_withdraw_checkout_confirm"),
    WITHDRAW_CHECKOUT_CANCEL("cash_withdraw_checkout_cancel"),
    WITHDRAW_SUCCESS("cash_withdraw_success"),
    WITHDRAW_ERROR("cash_withdraw_error")
}

fun withdrawEvent(event: WithdrawAnalytics, currency: String): AnalyticsEvent =
    object : AnalyticsEvent {
        override val event: String = event.event
        override val params: Map<String, String> = mapOf(
            "currency" to currency
        )
    }