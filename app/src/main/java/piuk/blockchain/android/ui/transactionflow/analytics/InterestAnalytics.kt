package piuk.blockchain.android.ui.transactionflow.analytics

import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import com.blockchain.notifications.analytics.LaunchOrigin
import info.blockchain.balance.Money
import java.io.Serializable

sealed class InterestAnalytics(
    override val event: String,
    override val params: Map<String, Serializable> = emptyMap(),
    override val origin: LaunchOrigin? = null
) : AnalyticsEvent {
    object InterestAnnouncementCta : InterestAnalytics("earn_banner_clicked")
    object InterestDashboardKyc : InterestAnalytics("earn_verify_identity_clicked")
    object InterestDashboardAction : InterestAnalytics("earn_interest_clicked")
    object InterestSummaryDepositCta : InterestAnalytics("earn_deposit_clicked")
    object InterestSummaryWithdrawCta : InterestAnalytics("earn_withdraw_clicked")
    object InterestClicked : InterestAnalytics(AnalyticsNames.INTEREST_CLICKED.eventName)

    class InterestDepositAmountEntered(
        currency: String,
        sourceAccountType: TxFlowAnalyticsAccountType,
        inputAmount: Money
    ) : InterestAnalytics(
        event = AnalyticsNames.INTEREST_DEPOSIT_AMOUNT_ENTERED.eventName,
        mapOf(
            CURRENCY to currency,
            SOURCE_ACCOUNT_TYPE to sourceAccountType.name,
            INPUT_AMOUNT to inputAmount.toBigDecimal()
        )
    )

    class InterestDepositClicked(
        currency: String,
        origin: LaunchOrigin
    ) : InterestAnalytics(
        event = AnalyticsNames.INTEREST_DEPOSIT_CLICKED.eventName,
        mapOf(
            CURRENCY to currency
        ),
        origin = origin
    )

    class InterestDepositMaxAmount(
        currency: String,
        sourceAccountType: TxFlowAnalyticsAccountType
    ) : InterestAnalytics(
        event = AnalyticsNames.INTEREST_MAX_CLICKED.eventName,
        mapOf(
            CURRENCY to currency,
            SOURCE_ACCOUNT_TYPE to sourceAccountType.name
        )
    )

    object InterestDepositViewed : InterestAnalytics(
        event = AnalyticsNames.INTEREST_DEPOSIT_VIEWED.eventName
    )

    object InterestViewed : InterestAnalytics(
        event = AnalyticsNames.INTEREST_VIEWED.eventName
    )

    class InterestWithdrawalClicked(
        currency: String,
        origin: LaunchOrigin
    ) : InterestAnalytics(
        event = AnalyticsNames.INTEREST_WITHDRAWAL_CLICKED.eventName,
        mapOf(
            CURRENCY to currency
        ),
        origin = origin
    )

    object InterestWithdrawalViewed : InterestAnalytics(
        event = AnalyticsNames.INTEREST_WITHDRAWAL_VIEWED.eventName
    )

    companion object {
        private const val CURRENCY = "currency"
        private const val SOURCE_ACCOUNT_TYPE = "from_account_type"
        private const val INPUT_AMOUNT = "input_amount"
    }
}