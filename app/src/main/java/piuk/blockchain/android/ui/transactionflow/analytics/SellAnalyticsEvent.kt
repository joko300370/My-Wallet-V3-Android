package piuk.blockchain.android.ui.transactionflow.analytics

import com.blockchain.notifications.analytics.AnalyticsEvent
import info.blockchain.balance.CryptoCurrency

sealed class SellAnalyticsEvent(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {

    object ConfirmationsDisplayed : SellAnalyticsEvent("sell_checkout_shown")
    object ConfirmTransaction : SellAnalyticsEvent("send_summary_confirm")
    object CancelTransaction : SellAnalyticsEvent("sell_checkout_cancel")
    object TransactionFailed : SellAnalyticsEvent("sell_checkout_error")
    object TransactionSuccess : SellAnalyticsEvent("sell_checkout_success")

    data class EnterAmountCtaClick(
        val asset: CryptoCurrency
    ) : SellAnalyticsEvent(
        "sell_amount_confirm_click",
        mapOf(
            PARAM_ASSET to asset.networkTicker
        )
    )

    companion object {
        private const val PARAM_ASSET = "asset"
    }
}
