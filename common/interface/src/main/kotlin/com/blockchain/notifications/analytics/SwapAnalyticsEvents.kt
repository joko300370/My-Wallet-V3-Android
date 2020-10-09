package com.blockchain.notifications.analytics

sealed class SwapAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = mapOf()
) : AnalyticsEvent {

    object SwapTabItemClick : SwapAnalyticsEvents("swap_tab_item_click")
    object SwapIntroStartButtonClick : SwapAnalyticsEvents("swap_intro_start_button_click")
    object SwapFormConfirmClick : SwapAnalyticsEvents("swap_form_confirm_click")
    object SwapFormConfirmErrorAppear : SwapAnalyticsEvents("swap_form_confirm_error_appear")
    object SwapFormConfirmErrorClick : SwapAnalyticsEvents("swap_form_confirm_error_click")
    object SwapSummaryConfirmClick : SwapAnalyticsEvents("swap_summary_confirm_click")
    object SwapSummaryConfirmFailure : SwapAnalyticsEvents("swap_summary_confirm_failure")
    object SwapSummaryConfirmSuccess : SwapAnalyticsEvents("swap_summary_confirm_success")
    object SwapReversePairClick : SwapAnalyticsEvents("swap_reverse_pair_click")
    object SwapLeftAssetClick : SwapAnalyticsEvents("swap_left_asset_click")
    object SwapRightAssetClick : SwapAnalyticsEvents("swap_right_asset_click")
    object SwapViewHistoryButtonClick : SwapAnalyticsEvents("swap_view_history_button_click")
    object SwapHistoryOrderClick : SwapAnalyticsEvents("swap_history_order_click")
    object SwapExchangeReceiveChange : SwapAnalyticsEvents("swap_exchange_change_received")
    object SwapMaxValueUsed : SwapAnalyticsEvents("swap_use_max")
}
