package piuk.blockchain.android.ui.transactionflow.analytics

import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import com.blockchain.notifications.analytics.LaunchOrigin
import info.blockchain.balance.Money
import java.io.Serializable

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
    WITHDRAW_ERROR("cash_withdraw_error");

    class WithdrawalAmountEntered(
        private val netAmount: Money,
        private val grossAmount: Money,
        private val paymentMethodType: PaymentMethodType
    ) : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.WITHDRAWAL_AMOUNT_ENTERED.eventName
        override val params: Map<String, Serializable>
            get() = mapOf(
                "currency" to netAmount.currencyCode,
                "input_amount" to grossAmount.toBigDecimal(),
                "output_amount" to netAmount.toBigDecimal(),
                "withdrawal_method" to paymentMethodType.name
            )
    }

    class WithdrawalMaxClicked(
        private val currency: String,
        private val paymentMethodType: PaymentMethodType
    ) : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.WITHDRAWAL_MAX_CLICKED.eventName
        override val params: Map<String, Serializable>
            get() = mapOf(
                "currency" to currency,
                "withdrawal_method" to paymentMethodType.name
            )
    }

    class WithdrawalClicked(
        override val origin: LaunchOrigin
    ) : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.WITHDRAWAL_CLICKED.eventName
        override val params: Map<String, Serializable>
            get() = mapOf()
    }

    class WithdrawMethodSelected(
        private val currency: String,
        private val paymentMethodType: PaymentMethodType
    ) : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.WITHDRAWAL_METHOD_SELECTED.eventName
        override val params: Map<String, Serializable>
            get() = mapOf(
                "currency" to currency,
                "withdrawal_method" to paymentMethodType.name
            )
    }
}

fun withdrawEvent(event: WithdrawAnalytics, currency: String): AnalyticsEvent =
    object : AnalyticsEvent {
        override val event: String = event.event
        override val params: Map<String, String> = mapOf(
            "currency" to currency
        )
    }