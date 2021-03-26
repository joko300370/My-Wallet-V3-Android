package piuk.blockchain.android.ui.home.analytics

import piuk.blockchain.android.R
import com.blockchain.notifications.analytics.AnalyticsEvent

data class SideNavEvent(private val menuItemId: Int) : AnalyticsEvent {
    override val event: String
        get() = "side_nav_$analyticsKey"

    override val params: Map<String, String> = emptyMap()

    private val analyticsKey: String = when (menuItemId) {
        R.id.nav_debug_swap -> "swap_debug"
        R.id.nav_the_exchange -> "pit"
        R.id.nav_airdrops -> "airdrops"
        R.id.nav_addresses -> "accounts_and_addresses"
        R.id.login_web_wallet -> "web_login"
        R.id.nav_settings -> "settings"
        R.id.nav_support -> "support"
        R.id.nav_logout -> "logout"
        else -> "unknown"
    }

    companion object SideMenuOpenEvent : AnalyticsEvent {
        override val event: String
            get() = "side_nav_shown"

        override val params: Map<String, String> = emptyMap()
    }
}