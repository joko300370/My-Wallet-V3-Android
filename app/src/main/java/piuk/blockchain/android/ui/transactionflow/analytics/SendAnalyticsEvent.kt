package piuk.blockchain.android.ui.transactionflow.analytics

import com.blockchain.notifications.analytics.AnalyticsEvent
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.coincore.FeeLevel

sealed class SendAnalyticsEvent(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {

    object EnterAddressDisplayed : SendAnalyticsEvent("send_address_screen_seen")
    object QrCodeScanned : SendAnalyticsEvent("send_form_qr_button_click")
    object EnterAddressCtaClick : SendAnalyticsEvent("send_address_click_confirm")
    object EnterAmountDisplayed : SendAnalyticsEvent("send_enter_amount_seen")
    object SendMaxClicked : SendAnalyticsEvent("send_max_amount_clicked")
    object EnterAmountCtaClick : SendAnalyticsEvent("send_enter_amount_confirm")
    object ConfirmationsDisplayed : SendAnalyticsEvent("send_summary_shown")
    object CancelTransaction : SendAnalyticsEvent("send_summary_cancel")
    object TransactionFailed : SendAnalyticsEvent("send_confirm_error")

    data class ConfirmTransaction(
        val asset: CryptoCurrency,
        val source: String,
        val target: String,
        val feeLevel: String
    ) : SendAnalyticsEvent(
        "send_summary_confirm",
        mapOf(
            PARAM_ASSET to asset.networkTicker,
            PARAM_SOURCE to source,
            PARAM_TARGET to target,
            FEE_SCHEDULE to feeLevel
        )
    )

    data class TransactionSuccess(val asset: CryptoCurrency) :
        SendAnalyticsEvent(
            "send_confirm_success",
            mapOf(
                PARAM_ASSET to asset.networkTicker
            )
        )

    data class FeeChanged(val oldFee: FeeLevel, val newFee: FeeLevel) :
        SendAnalyticsEvent(
            "send_change_fee_click",
            mapOf(
                PARAM_OLD_FEE to oldFee.name,
                PARAM_NEW_FEE to newFee.name
            )
        )

    companion object {
        private const val PARAM_ASSET = "asset"
        private const val PARAM_SOURCE = "source"
        private const val PARAM_TARGET = "target"
        private const val PARAM_OLD_FEE = "old_fee"
        private const val PARAM_NEW_FEE = "new_fee"
        private const val FEE_SCHEDULE = "fee_level"
    }
}
