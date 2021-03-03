package piuk.blockchain.android.sdd

import com.blockchain.notifications.analytics.AnalyticsEvent

enum class SDDAnalytics(override val event: String, override val params: Map<String, String> = emptyMap()) :
    AnalyticsEvent {
    SDD_ELIGIBLE("user_is_sdd_eligible"),
    UPGRADE_TO_GOLD_CLICKED("upgrade_to_gold_clicked"),
    UPGRADE_TO_GOLD_SEEN("upgrade_to_gold_seen")
}