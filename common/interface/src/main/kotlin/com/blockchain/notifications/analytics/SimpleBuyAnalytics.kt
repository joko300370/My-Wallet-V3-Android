package com.blockchain.notifications.analytics

import info.blockchain.balance.CryptoCurrency

enum class SimpleBuyAnalytics(override val event: String, override val params: Map<String, String> = emptyMap()) :
    AnalyticsEvent {

    INTRO_SCREEN_SHOW("sb_screen_shown"),
    I_WANT_TO_BUY_CRYPTO_BUTTON_CLICKED("sb_button_clicked"),
    SKIP_ALREADY_HAVE_CRYPTO("sb_button_skip"),
    I_WANT_TO_BUY_CRYPTO_ERROR("sb_want_to_buy_screen_error"),

    BUY_FORM_SHOWN("sb_buy_form_shown"),

    START_GOLD_FLOW("sb_kyc_start"),
    KYC_VERIFYING("sb_kyc_verifying"),
    KYC_MANUAL("sb_kyc_manual_review"),
    KYC_PENDING("sb_kyc_pending"),
    KYC_NOT_ELIGIBLE("sb_post_kyc_not_eligible"),

    CHECKOUT_SUMMARY_SHOWN("sb_checkout_shown"),
    CHECKOUT_SUMMARY_CONFIRMED("sb_checkout_confirm"),
    CHECKOUT_SUMMARY_PRESS_CANCEL("sb_checkout_cancel"),
    CHECKOUT_SUMMARY_CANCELLATION_CONFIRMED("sb_checkout_cancel_confirmed"),
    CHECKOUT_SUMMARY_CANCELLATION_GO_BACK("sb_checkout_cancel_go_back"),

    BANK_DETAILS_FINISHED("sb_bank_details_finished"),

    PENDING_TRANSFER_MODAL_CANCEL_CLICKED("sb_pending_modal_cancel_click"),

    CUSTODY_WALLET_CARD_SHOWN("sb_custody_wallet_card_shown"),
    CUSTODY_WALLET_CARD_CLICKED("sb_custody_wallet_card_clicked"),

    BACK_UP_YOUR_WALLET_SHOWN("sb_backup_wallet_card_shown"),
    BACK_UP_YOUR_WALLET_CLICKED("sb_backup_wallet_card_clicked"),

    WITHDRAW_WALLET_SCREEN_SUCCESS("sb_withdrawal_screen_success"),
    WITHDRAW_WALLET_SCREEN_FAILURE("sb_withdrawal_screen_failure"),

    BANK_DETAILS_CANCEL_PROMPT("sb_cancel_order_prompt"),
    BANK_DETAILS_CANCEL_CONFIRMED("sb_cancel_order_confirmed"),
    BANK_DETAILS_CANCEL_GO_BACK("sb_cancel_order_go_back"),
    BANK_DETAILS_CANCEL_ERROR("sb_cancel_order_error"),

    SELECT_YOUR_CURRENCY_SHOWN("sb_currency_select_screen"),
    CURRENCY_NOT_SUPPORTED_SHOWN("sb_currency_unsupported"),
    CURRENCY_NOT_SUPPORTED_CHANGE("sb_unsupported_change_currency"),
    CURRENCY_NOT_SUPPORTED_SKIP("sb_unsupported_view_home"),

    ADD_CARD("sb_add_card_screen_shown"),
    CARD_INFO_SET("sb_card_info_set"),
    CARD_BILLING_ADDRESS_SET("sb_billing_address_set"),
    CARD_3DS_COMPLETED("sb_three_d_secure_complete"),
    PAYMENT_METHODS_SHOWN("sb_payment_method_shown"),
    REMOVE_CARD("sb_remove_card"),

    SETTINGS_ADD_CARD("sb_settings_add_card_clicked"),

    REMOVE_BANK("sb_remove_bank"),

    LINK_BANK_CLICKED("sb_link_bank_clicked"),
    LINK_BANK_LOADING_ERROR("sb_link_bank_loading_error"),
    LINK_BANK_SCREEN_SHOWN("sb_link_bank_screen_shown"),

    WITHDRAWAL_FORM_SHOWN("cash_withdraw_form_shown"),
    WITHDRAWAL_CONFIRM_AMOUNT("cash_witdraw_form_confirm_click"),
    WITHDRAWAL_CHECKOUT_SHOWN("cash_withdraw_form_shown"),
    WITHDRAWAL_CHECKOUT_CONFIRM("cash_withdraw_checkout_confirm"),
    WITHDRAWAL_CHECKOUT_CANCEL("cash_withdraw_checkout_cancel"),
    WITHDRAWAL_SUCCESS("cash_withdraw_success"),
    WITHDRAWAL_ERROR("cash_withdraw_error"),
}

fun buyConfirmClicked(amount: String, fiatCurrency: String, paymentMethod: String): AnalyticsEvent =
    object : AnalyticsEvent {
        override val event: String = "sb_buy_form_confirm_click"
        override val params: Map<String, String> = mapOf(
            "amount" to amount,
            "paymentMethod" to paymentMethod,
            "currency" to fiatCurrency
        )
    }

fun eventWithPaymentMethod(analytics: SimpleBuyAnalytics, paymentMethod: String): AnalyticsEvent =
    object : AnalyticsEvent {
        override val event: String = analytics.event
        override val params: Map<String, String> = mapOf(
            "paymentMethod" to paymentMethod
        )
    }

fun withdrawEventWithCurrency(analytics: SimpleBuyAnalytics, currency: String, amount: String? = null): AnalyticsEvent =
    object : AnalyticsEvent {
        override val event: String = analytics.event
        override val params: Map<String, String> = mutableMapOf(
            "currency" to currency
        ).apply {
            amount?.let {
                this.put("amount", amount)
            }
        }.toMap()
    }

class BankDetailsViewed(fiatCurrency: String) : AnalyticsEvent {
    override val event: String = "sb_bank_details_shown"
    override val params: Map<String, String> = mapOf(
        "currency" to fiatCurrency
    )
}

class CustodialBalanceClicked(cryptoCurrency: CryptoCurrency) : AnalyticsEvent {
    override val event: String = "sb_trading_wallet_clicked"
    override val params: Map<String, String> = mapOf(
        "asset" to cryptoCurrency.networkTicker
    )
}

class PaymentMethodSelected(paymentMethod: String) : AnalyticsEvent {
    override val event: String = "sb_payment_method_selected"
    override val params: Map<String, String> = mapOf(
        "selection" to paymentMethod
    )
}

class CustodialBalanceSendClicked(cryptoCurrency: CryptoCurrency) : AnalyticsEvent {
    override val event: String = "sb_trading_wallet_send"
    override val params: Map<String, String> = mapOf(
        "asset" to cryptoCurrency.networkTicker
    )
}

fun bankFieldName(field: String): AnalyticsEvent = object : AnalyticsEvent {
    override val event: String = "sb_bank_details_copied"
    override val params: Map<String, String> = mapOf(
        "field" to field
    )
}

fun linkBankFieldCopied(field: String, currency: String): AnalyticsEvent = object : AnalyticsEvent {
    override val event: String = "sb_link_bank_details_copied"
    override val params: Map<String, String> = mapOf(
        "field" to field,
        "currency" to currency
    )
}

fun linkBankEventWithCurrency(analytics: SimpleBuyAnalytics, currency: String): AnalyticsEvent =
    object : AnalyticsEvent {
        override val event: String = analytics.event
        override val params: Map<String, String> = mapOf(
            "currency" to currency
        )
    }

class PendingTransactionShown(fiatCurrency: String) : AnalyticsEvent {
    override val event: String = "sb_pending_modal_shown"
    override val params: Map<String, String> = mapOf(
        "currency" to fiatCurrency
    )
}

class WithdrawScreenShown(cryptoCurrency: CryptoCurrency) : AnalyticsEvent {
    override val event: String = "sb_withdrawal_screen_shown"
    override val params: Map<String, String> = mapOf(
        "asset" to cryptoCurrency.networkTicker
    )
}

class CurrencySelected(fiatCurrency: String) : AnalyticsEvent {
    override val event: String = "sb_currency_selected"
    override val params: Map<String, String> = mapOf(
        "currency" to fiatCurrency
    )
}

class CurrencyChangedFromBuyForm(fiatCurrency: String) : AnalyticsEvent {
    override val event: String = "sb_buy_form_fiat_changed"
    override val params: Map<String, String> = mapOf(
        "currency" to fiatCurrency
    )
}

class WithdrawScreenClicked(cryptoCurrency: CryptoCurrency) : AnalyticsEvent {
    override val event: String = "sb_withdrawal_screen_clicked"
    override val params: Map<String, String> = mapOf(
        "asset" to cryptoCurrency.networkTicker
    )
}