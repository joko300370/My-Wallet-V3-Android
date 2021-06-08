package piuk.blockchain.android.ui.transactionflow.analytics

import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import com.blockchain.notifications.analytics.LaunchOrigin

sealed class DepositAnalytics(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {

    class DepositClicked(override val origin: LaunchOrigin) : DepositAnalytics(
        event = AnalyticsNames.DEPOSIT_CLICKED.eventName
    )

    class DepositAmountEntered(
        currency: String
    ) : DepositAnalytics(
        event = AnalyticsNames.DEPOSIT_AMOUNT_ENTERED.eventName,
        params = mapOf(
            "currency" to currency
        )
    )

    class DepositMethodSelected(
        currency: String
    ) : DepositAnalytics(
        event = AnalyticsNames.DEPOSIT_METHOD_SELECTED.eventName,
        params = mapOf(
            "currency" to currency
        )
    )
}