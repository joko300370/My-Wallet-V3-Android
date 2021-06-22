package com.blockchain.notifications.analytics

@Deprecated("Analytics events should be defined near point of use")
enum class AnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {

    AccountsAndAddresses("accounts_and_addresses"),
    Backup("backup"),
    Dashboard("dashboard"),
    Exchange("exchange"),
    ExchangeCreate("exchange_create"),
    ExchangeDetailConfirm("exchange_detail_confirm"),
    ExchangeDetailOverview("exchange_detail_overview"),
    ExchangeExecutionError("exchange_execution_error"),
    ExchangeHistory("exchange_history"),
    KycEmail("kyc_email"),
    KycAddress("kyc_address"),
    KycComplete("kyc_complete"),
    SwapTiers("swap_tiers"),
    KycTiersLocked("kyc_tiers_locked"),
    KycTier1Complete("kyc_tier1_complete"),
    KycTier2Complete("kyc_tier2_complete"),
    KycCountry("kyc_country"),
    KycProfile("kyc_profile"),
    KycStates("kyc_states"),
    KycVerifyIdentity("kyc_verify_identity"),
    KycWelcome("kyc_welcome"),
    KycResubmission("kyc_resubmission"),
    KycSunriverStart("kyc_sunriver_start"),
    KycBlockstackStart("kyc_blockstack_start"),
    KycSimpleBuyStart("kyc_simple_buy_start"),
    KycFiatFundsStart("kyc_fiat_funds_start"),
    KycInterestStart("kyc_interest_start"),
    KycMoreInfo("kyc_more_info"),
    KycTiers("kyc_tiers"),
    Logout("logout"),
    Settings("settings"),
    Support("support"),
    WebLogin("web_login"),
    SwapErrorDialog("swap_error_dialog"),
    SwapErrorDialogCtaClicked("swap_error_dialog_cta_clicked"),
    SwapErrorDialogDismissClicked("swap_error_dialog_dismiss_clicked"),
    WalletCreation("wallet_creation"),
    WalletManualLogin("wallet_manual_login"),
    PITDEEPLINK("pit_deeplink"),
    WalletAutoPairing("wallet_auto_pairing"),
    ChangeFiatCurrency("currency"),
    OpenAssetsSelector("asset_selector_open"),
    CloseAssetsSelector("asset_selector_open"),
    CameraSystemPermissionApproved("permission_sys_camera_approve"),
    CameraSystemPermissionDeclined("permission_sys_camera_decline"),

    WalletSignupOpen("wallet_signup_open"),
    WalletSignupClickCreate("wallet_signup_create"),
    WalletSignupClickEmail("wallet_signup_email"),
    WalletSignupClickPasswordFirst("wallet_signup_password_first"),
    WalletSignupClickPasswordSecond("wallet_signup_password_second"),
    WalletSignupCreated("wallet_signup_wallet_created"),
    WalletSignupPINFirst("wallet_signup_pin_first"),
    WalletSignupPINSecond("wallet_signup_pin_second"),
    WalletSignupFirstLogIn("wallet_signup_login")
}

fun kycTierStart(tier: Int): AnalyticsEvent = object : AnalyticsEvent {
    override val event: String = "kyc_tier${tier}_start"
    override val params: Map<String, String> = emptyMap()
}

fun networkError(host: String, path: String, message: String): AnalyticsEvent = object : AnalyticsEvent {
    override val event: String
        get() = "network_error"
    override val params: Map<String, String>
        get() = mapOf("host" to host, "message" to message, "path" to path)
}

fun apiError(host: String, path: String, body: String?, requestId: String?, errorCode: Int): AnalyticsEvent =
    object : AnalyticsEvent {
        override val event: String
            get() = "api_error"
        override val params: Map<String, String>
            get() = mapOf(
                "host" to host,
                "body" to body,
                "path" to path,
                "error_code" to errorCode.toString(),
                "request_id" to requestId
            ).mapNotNull { it.value?.let { value -> it.key to value } }.toMap()
    }

enum class AnalyticsNames(val eventName: String) {
    BUY_AMOUNT_ENTERED("Buy Amount Entered"),
    BUY_PAYMENT_METHOD_CHANGED("Buy Payment Method Selected"),
    BUY_SELL_CLICKED("Buy Sell Clicked"),
    BUY_SELL_VIEWED("Buy Sell Viewed"),
    SIGNED_IN("Signed In"),
    SIGNED_OUT("Signed Out"),
    SWAP_VIEWED("Swap Viewed"),
    SWAP_CLICKED("Swap Clicked"),
    SWAP_RECEIVE_SELECTED("Swap Receive Selected"),
    SWAP_MAX_CLICKED("Swap Amount Max Clicked"),
    SWAP_FROM_SELECTED("Swap From Selected"),
    SWAP_ACCOUNTS_SELECTED("Swap Accounts Selected"),
    SWAP_AMOUNT_ENTERED("Swap Amount Entered"),
    AMOUNT_SWITCHED("Amount Switched"),
    SWAP_REQUESTED("Swap Requested"),
    SEND_MAX_CLICKED("Send Amount Max Clicked"),
    SEND_RECEIVE_CLICKED("Send Receive Clicked"),
    SEND_FROM_SELECTED("Send From Selected"),
    SEND_SUBMITTED("Send Submitted"),
    SEND_RECEIVE_VIEWED("Send Receive Viewed"),
    RECEIVE_ACCOUNT_SELECTED("Receive Currency Selected"),
    RECEIVE_ADDRESS_COPIED("Receive Details Copied"),
    WITHDRAWAL_AMOUNT_ENTERED("Withdrawal Amount Entered"),
    WITHDRAWAL_MAX_CLICKED("Withdrawal Amount Max Clicked"),
    WITHDRAWAL_CLICKED("Withdrawal Clicked"),
    WITHDRAWAL_METHOD_SELECTED("Withdrawal Method Selected"),
    LINK_BANK_CONDITIONS_APPROVED("Link Bank Conditions Approved"),
    LINK_BANK_CLICKED("Link Bank Clicked"),
    BANK_SELECTED("Link Bank Selected"),
    SELL_AMOUNT_ENTERED("Sell Amount Entered"),
    SELL_SOURCE_SELECTED("Sell From Selected"),
    SELL_AMOUNT_MAX_CLICKED("Sell Amount Max Clicked"),
    DEPOSIT_CLICKED("Deposit Clicked"),
    DEPOSIT_AMOUNT_ENTERED("Deposit Amount Entered"),
    DEPOSIT_METHOD_SELECTED("Deposit Method Selected"),
    INTEREST_CLICKED("Interest Clicked"),
    INTEREST_DEPOSIT_AMOUNT_ENTERED("Interest Deposit Amount Entered"),
    INTEREST_DEPOSIT_CLICKED("Interest Deposit Clicked"),
    INTEREST_MAX_CLICKED("Interest Deposit Max Amount Clicked"),
    INTEREST_DEPOSIT_VIEWED("Interest Deposit Viewed"),
    INTEREST_VIEWED("Interest Viewed"),
    INTEREST_WITHDRAWAL_CLICKED("Interest Withdrawal Clicked"),
    INTEREST_WITHDRAWAL_VIEWED("Interest Withdrawal Viewed"),
    EMAIL_VERIFF_REQUESTED("Email Verification Requested");
}

enum class LaunchOrigin {
    NAVIGATION,
    SEND,
    SWAP,
    AIRDROP,
    RESUBMISSION,
    SIMPLETRADE,
    DASHBOARD_PROMO,
    TRANSACTION_DETAILS,
    DEPOSIT,
    WITHDRAWAL,
    CURRENCY_PAGE,
    SAVINGS,
    FIAT_FUNDS,
    SIGN_UP,
    SETTINGS,
    SAVINGS_PAGE,
    VERIFICATION;
}