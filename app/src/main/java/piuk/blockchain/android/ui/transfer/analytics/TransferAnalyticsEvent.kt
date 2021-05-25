package piuk.blockchain.android.ui.transfer.analytics

import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import com.blockchain.notifications.analytics.LaunchOrigin
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.ui.transactionflow.analytics.toCategory
import java.io.Serializable

sealed class TransferAnalyticsEvent(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {

    object NoBalanceViewDisplayed : TransferAnalyticsEvent("send_no_balance_seen")
    object NoBalanceCtaClicked : TransferAnalyticsEvent("send_no_balance_buy_clicked")

    data class SourceWalletSelected(
        val wallet: CryptoAccount
    ) : TransferAnalyticsEvent(
        "send_wallet_select",
        mapOf(
            PARAM_ASSET to wallet.asset.networkTicker,
            PARAM_WALLET to (wallet as BlockchainAccount).toCategory()
        )
    )

    class TransferClicked(
        override val origin: LaunchOrigin,
        private val type: AnalyticsTransferType
    ) : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.SEND_RECEIVE_CLICKED.name
        override val params: Map<String, Serializable>
            get() = mapOf(
                "type" to type.name
            )
    }

    object TransferViewed : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.SEND_RECEIVE_CLICKED.name
        override val params: Map<String, Serializable>
            get() = mapOf()
    }

    companion object {
        private const val PARAM_ASSET = "asset"
        private const val PARAM_WALLET = "wallet"
    }

    enum class AnalyticsTransferType {
        SEND, RECEIVE
    }
}
