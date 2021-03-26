package piuk.blockchain.android.ui.dashboard

import info.blockchain.balance.CryptoCurrency
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HideAssetDetailsTest {

    val subject = ClearBottomSheet

    @Test
    fun `clearing empty asset sheet no effect`() {
        val initialState = DashboardState(
            assets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            dashboardNavigationAction = null,
            announcement = testAnnouncementCard_1
        )

        val result = subject.reduce(initialState)
        assertEquals(result, initialState)
    }

    @Test
    fun `clearing asset sheet, clears the asset and leaves other fields unchanged`() {

        val initialState = DashboardState(
            assets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            dashboardNavigationAction = null,
            announcement = testAnnouncementCard_1
        )

        val result = subject.reduce(initialState)

        assertEquals(result.assets, initialState.assets)
        assertNull(result.dashboardNavigationAction)
        assertEquals(result.announcement, initialState.announcement)
    }

    @Test
    fun `clearing promo sheet, clears the sheet and leaves other fields unchanged`() {

        val initialState = DashboardState(
            assets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            dashboardNavigationAction = DashboardNavigationAction.StxAirdropComplete,
            announcement = testAnnouncementCard_1
        )

        val result = subject.reduce(initialState)

        assertEquals(result.assets, initialState.assets)
        assertNull(result.dashboardNavigationAction)
        assertEquals(result.announcement, initialState.announcement)
    }
}
