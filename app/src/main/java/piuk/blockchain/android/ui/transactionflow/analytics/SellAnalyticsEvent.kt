package piuk.blockchain.android.ui.transactionflow.analytics

import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics.Companion.constructMap
import java.io.Serializable

class SellAnalyticsEvent(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {

    constructor(event: SellAnalytics, asset: CryptoCurrency, source: String) : this(
        event.value, constructMap(
            asset = asset, source = source, target = WALLET_TYPE_CUSTODIAL
        )
    )
}

enum class SellAnalytics(internal val value: String) {
    ConfirmationsDisplayed("sell_checkout_shown"),
    ConfirmTransaction("send_summary_confirm"),
    CancelTransaction("sell_checkout_cancel"),
    TransactionFailed("sell_checkout_error"),
    TransactionSuccess("sell_checkout_success"),
    EnterAmountCtaClick("sell_amount_confirm_click")
}

class AmountEntered(
    private val sourceAccountType: TxFlowAnalyticsAccountType,
    private val amount: Money,
    private val outputCurrency: String
) : AnalyticsEvent {
    override val event: String
        get() = AnalyticsNames.SELL_AMOUNT_ENTERED.eventName
    override val params: Map<String, Serializable>
        get() = mapOf(
            FROM_ACCOUNT_TYPE to sourceAccountType.name,
            INPUT_AMOUNT to amount.toBigDecimal(),
            INPUT_CURRENCY to amount.currencyCode,
            OUTPUT_CURRENCY to outputCurrency
        )
}

class MaxAmountClicked(
    private val sourceAccountType: TxFlowAnalyticsAccountType,
    private val inputCurrency: String,
    private val outputCurrency: String
) : AnalyticsEvent {
    override val event: String
        get() = AnalyticsNames.SELL_AMOUNT_MAX_CLICKED.eventName
    override val params: Map<String, Serializable>
        get() = mapOf(
            FROM_ACCOUNT_TYPE to sourceAccountType.name,
            INPUT_CURRENCY to inputCurrency,
            OUTPUT_CURRENCY to outputCurrency
        )
}

class SellSourceAccountSelected(
    private val sourceAccountType: TxFlowAnalyticsAccountType,
    private val inputCurrency: String
) : AnalyticsEvent {
    override val event: String
        get() = AnalyticsNames.SELL_SOURCE_SELECTED.eventName
    override val params: Map<String, Serializable>
        get() = mapOf(
            FROM_ACCOUNT_TYPE to sourceAccountType.name,
            INPUT_CURRENCY to inputCurrency
        )
}

private const val FROM_ACCOUNT_TYPE = "from_account_type"
private const val INPUT_CURRENCY = "input_currency"
private const val OUTPUT_CURRENCY = "output_currency"
private const val INPUT_AMOUNT = "output_currency"
