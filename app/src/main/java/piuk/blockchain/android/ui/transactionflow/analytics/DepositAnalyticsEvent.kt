package piuk.blockchain.android.ui.transactionflow.analytics

import com.blockchain.notifications.analytics.AnalyticsEvent

sealed class DepositAnalyticsEvent(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {
    object ConfirmTransaction : DepositAnalyticsEvent("earn_deposit_confirm_click")
}
