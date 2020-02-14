package com.blockchain.notifications.analytics

enum class SimpleBuyAnalytics(override val event: String, override val params: Map<String, String> = emptyMap()) :
    AnalyticsEvent {

    INTRO_SCREEN_SHOW("sb_screen_shown"),
    I_WANT_TO_BUY_CRYPTO_BUTTON_CLICKED("sb_button_clicked"),
    SKIP_ALREADY_HAVE_CRYPTO("sb_button_skip"),
    I_WANT_TO_BUY_CRYPTO_ERROR("sb_want_to_buy_screen_error"),

    BUY_FORM_SHOWN("sb_buy_form_shown"),
    BUY_MIN_CLICKED("sb_buy_min"),
    BUY_MAX_CLICKED("sb_buy_max"),
    BUY_CONFIRM_CLICKED("sb_form_confirm_click"),

    START_GOLD_FLOW("sb_kyc_start"),
    KYC_VERYFING("sb_kyc_verifying"),
    KYC_MANUAL("sb_kyc_manual_review"),
    KYC_PENDING("sb_kyc_pending"),
    KYC_NOT_ELIGIBLE("sb_kyc_not_eligible"),

    CHECKOUT_SUMMARY_SHOWN("sb_checkout_shown"),
    CHECKOUT_SUMMARY_CONFIRMED("sb_checkout_confirm"),
    CHECKOUT_SUMMARY_PRESS_CANCELL("sb_checkout_cancel"),
    CHECKOUT_SUMMARY_CANCELLATION_CONFIRMED("sb_checkout_cancel_confirmed"),
    CHECKOUT_SUMMARY_CANCELLATION_GO_BACK("sb_checkout_cancel_go_back"),

    BANK_DETAILS_FINISHED("sb_bank_details_finished"),

    PENDING_TRANSFER_MODAL_CANCEL_CLICKED("sb_pending_modal_cancel_click"),

    CUSTODY_WALLET_CARD_SHOWN("sb_custody_wallet_card_shown"),
    CUSTODY_WALLET_CARD_CLICKED("sb_custody_wallet_card_clicked"),

    BACK_UP_YOUR_WALLET_SHOWN("sb_backup_wallet_card_shown"),
    BACK_UP_YOUR_WALLET_CLCIKED("sb_backup_wallet_card_clicked"),
    WITHDRAW_WALLET_SCREEN_SHOW("sb_withdrawal_screen_shown"),

    WITHDRAW_WALLET_SCREEN_SUCCESS("sb_withdrawal_screen_success"),
    WITHDRAW_WALLET_SCREEN_FAILURE("sb_withdrawal_screen_failure")
}

fun buyConfirmClicked(amount: String, currency: String): AnalyticsEvent = object : AnalyticsEvent {
    override val event: String = "sb_buy_form_confirm_click"
    override val params: Map<String, String> = mapOf(
        "amount" to amount,
        "currency" to currency
    )
}

fun cryptoChanged(asset: String): AnalyticsEvent = object : AnalyticsEvent {
    override val event: String = "sb_buy_form_crypto_changed"
    override val params: Map<String, String> = mapOf(
        "asset" to asset
    )
}

fun bankDetailsShow(currency: String): AnalyticsEvent = object : AnalyticsEvent {
    override val event: String = "sb_bank_details_shown"
    override val params: Map<String, String> = mapOf(
        "currency" to currency
    )
}

fun bankFieldName(field: String): AnalyticsEvent = object : AnalyticsEvent {
    override val event: String = "sb_bank_details_copied"
    override val params: Map<String, String> = mapOf(
        "field" to field
    )
}

fun pendingModalShow(currency: String): AnalyticsEvent = object : AnalyticsEvent {
    override val event: String = "sb_pending_modal_shown"
    override val params: Map<String, String> = mapOf(
        "currency" to currency
    )
}

fun withDrawScreenShow(asset: String): AnalyticsEvent = object : AnalyticsEvent {
    override val event: String = "sb_withdrawal_screen_shown"
    override val params: Map<String, String> = mapOf(
        "asset" to asset
    )
}

fun withDrawScreenClicked(asset: String): AnalyticsEvent = object : AnalyticsEvent {
    override val event: String = "sb_withdrawal_screen_clicked"
    override val params: Map<String, String> = mapOf(
        "asset" to asset
    )
}