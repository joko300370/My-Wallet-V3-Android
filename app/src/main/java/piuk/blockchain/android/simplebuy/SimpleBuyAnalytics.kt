package piuk.blockchain.android.simplebuy

import com.blockchain.extensions.withoutNullValues
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import com.blockchain.notifications.analytics.LaunchOrigin
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import java.io.Serializable

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

    CUSTODY_WALLET_CARD_SHOWN("sb_custody_wallet_card_shown"),
    CUSTODY_WALLET_CARD_CLICKED("sb_custody_wallet_card_clicked"),

    BACK_UP_YOUR_WALLET_SHOWN("sb_backup_wallet_card_shown"),
    BACK_UP_YOUR_WALLET_CLICKED("sb_backup_wallet_card_clicked"),

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
    REMOVE_CARD("sb_remove_card"),

    SETTINGS_ADD_CARD("sb_settings_add_card_clicked"),

    REMOVE_BANK("sb_remove_bank"),

    WIRE_TRANSFER_CLICKED("sb_link_bank_clicked"),
    WIRE_TRANSFER_LOADING_ERROR("sb_link_bank_loading_error"),
    WIRE_TRANSFER_SCREEN_SHOWN("sb_link_bank_screen_shown"),

    ACH_SUCCESS("sb_ach_success");
}

fun PaymentMethod.toAnalyticsString(): String =
    when (this) {
        is PaymentMethod.Card,
        is PaymentMethod.UndefinedCard -> "CARD"
        is PaymentMethod.Funds,
        is PaymentMethod.UndefinedFunds -> "FUNDS"
        is PaymentMethod.Bank,
        is PaymentMethod.UndefinedBankTransfer -> "LINK_BANK"
        else -> ""
    }

fun PaymentMethod.toNabuAnalyticsString(): String =
    when (this) {
        is PaymentMethod.Card -> "PAYMENT_CARD"
        is PaymentMethod.Bank -> "BANK_TRANSFER"
        is PaymentMethod.Funds -> "FUNDS"
        else -> ""
    }

fun PaymentMethodType.toAnalyticsString() =
    when (this) {
        PaymentMethodType.PAYMENT_CARD -> "CARD"
        PaymentMethodType.FUNDS -> "FUNDS"
        PaymentMethodType.BANK_TRANSFER -> "LINK_BANK"
        else -> ""
    }

fun paymentMethodsShown(paymentMethods: String): AnalyticsEvent =
    object : AnalyticsEvent {
        override val event: String = "sb_payment_method_shown"
        override val params: Map<String, String> = mapOf(
            "options" to paymentMethods
        )
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

class BuyAmountEntered(inputAmount: Money, maxAmount: Money, outputCurrency: String) : AnalyticsEvent {
    override val event: String = AnalyticsNames.BUY_AMOUNT_ENTERED.eventName
    override val params: Map<String, Serializable> = mapOf(
        "input_amount" to inputAmount.toBigDecimal(),
        "input_currency" to inputAmount.currencyCode,
        "input_amount_max" to maxAmount.toBigDecimal(),
        "output_currency" to outputCurrency
    )
}

class BuySellViewedEvent(private val type: BuySellType? = null) : AnalyticsEvent {
    override val event: String
        get() = AnalyticsNames.BUY_SELL_VIEWED.eventName
    override val params: Map<String, Serializable>
        get() = mapOf(
            "type" to type?.name
        ).withoutNullValues()
}

class BuySellClicked(override val origin: LaunchOrigin, val type: BuySellType? = null) : AnalyticsEvent {

    override val event: String
        get() = AnalyticsNames.BUY_SELL_CLICKED.eventName
    override val params: Map<String, Serializable>
        get() = mapOf(
            "type" to type?.name
        ).withoutNullValues()
}

class BuyPaymentMethodSelected(type: String) : AnalyticsEvent {
    override val event: String = AnalyticsNames.BUY_PAYMENT_METHOD_CHANGED.eventName
    override val params: Map<String, Serializable> = mapOf(
        "payment_type" to type
    )
}

enum class BuySellType {
    BUY, SELL
}