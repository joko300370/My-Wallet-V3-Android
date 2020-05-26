package com.blockchain.notifications.analytics

enum class ActivityAnalytics(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) :
    AnalyticsEvent {
    WALLET_PICKER_SHOWN("activity_select_wallet_picker"),
    DETAILS_BUY_AWAITING_FUNDS("activity_buy_waiting_for_funds"),
    DETAILS_BUY_CANCEL("activity_cancel_buy_clicked"),
    DETAILS_BUY_PENDING("activity_buy_pending"),
    DETAILS_BUY_COMPLETE("activity_buy_complete"),
    DETAILS_BUY_PURCHASE_AGAIN("activity_buy_again_clicked"),
    DETAILS_SEND_CONFIRMING("activity_send_confirming"),
    DETAILS_SEND_COMPLETE("activity_send_complete"),
    DETAILS_SEND_VIEW_EXPLORER("activity_send_view_explorer"),
    DETAILS_RECEIVE_COMPLETE("activity_receive_complete"),
    DETAILS_RECEIVE_VIEW_EXPLORER("activity_receive_view_explorer"),
    // events have been added to support future swap implementation
    DETAILS_SWAP_PENDING("activity_swap_pending"),
    DETAILS_SWAP_COMPLETE("activity_swap_complete"),
    DETAILS_SWAP_VIEW_EXPLORER("activity_swap_view_explorer"),
    // airdrop is not supported yet
    DETAILS_AIRDROP_CLAIMED("activity_airdrop_claimed"),
    DETAILS_AIRDROP_RECEIVED("activity_airdrop_received"),
    DETAILS_AIRDROP_VIEW_EXPLORER("activity_airdrop_view_explorer"),
    DETAILS_FEE_PENDING("activity_gas_pending"),
    DETAILS_FEE_COMPLETE("activity_gas_complete"),
    DETAILS_FEE_VIEW_EXPLORER("activity_gas_view_explorer")
}

fun activityShown(activityType: String): AnalyticsEvent = object : AnalyticsEvent {
    override val event: String = "activity_page_shown"
    override val params: Map<String, String> = mapOf(
        "wallet" to activityType
    )
}