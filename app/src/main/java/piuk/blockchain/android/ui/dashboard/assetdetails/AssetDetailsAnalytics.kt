package piuk.blockchain.android.ui.dashboard.assetdetails

import com.blockchain.notifications.analytics.AnalyticsEvent

enum class AssetDetailsAnalytics(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {
    WALLET_DETAILS("wallet_detail_clicked"),
    SEND_CLICKED("wallet_detail_send_clicked"),
    RECEIVE_CLICKED("wallet_detail_receive_clicked"),
    SWAP_CLICKED("wallet_detail_swap_clicked"),
    ACTIVITY_CLICKED("wallet_detail_activity_clicked"),
    SELL_CLICKED("wallet_detail_sell_clicked"),
    FIAT_DETAIL_CLICKED("cash_wallet_detail_clicked"),
    FIAT_DEPOSIT_CLICKED("cash_wallet_deposit_clicked"),
    FIAT_WITHDRAW_CLICKED("cash_wallet_withdraw"),
    FIAT_ACTIVITY_CLICKED("cash_wallet_activity_clicked")
}

fun assetActionEvent(event: AssetDetailsAnalytics, ticker: String): AnalyticsEvent =
    object : AnalyticsEvent {
        override val event: String = event.event
        override val params: Map<String, String> = mapOf(
            "asset" to ticker
        )
    }

fun fiatAssetAction(event: AssetDetailsAnalytics, currency: String): AnalyticsEvent =
    object : AnalyticsEvent {
        override val event: String = event.event
        override val params: Map<String, String> = mapOf(
            "currency" to currency
        )
    }
