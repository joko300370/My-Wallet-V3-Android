package piuk.blockchain.android.ui.dashboard

import info.blockchain.balance.CryptoCurrency
import org.junit.Test
import kotlin.test.assertEquals

class ShowAssetDetailsTest {

    val subject = ShowCryptoAssetDetails(CryptoCurrency.ETHER)

    @Test
    fun `showing asset details, sets asset type and leaves other fields unchanged`() {

        val initialState = DashboardState(
            assets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            showAssetSheetFor = null,
            announcement = testAnnouncementCard_1
        )

        val result = subject.reduce(initialState)

        assertEquals(result.assets, initialState.assets)
        assertEquals(result.showAssetSheetFor, CryptoCurrency.ETHER)
        assertEquals(result.announcement, testAnnouncementCard_1)
    }

    @Test
    fun `replacing asset details type, sets asset and leaves other fields unchanged`() {

        val initialState = DashboardState(
            assets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            showAssetSheetFor = CryptoCurrency.ETHER,
            announcement = testAnnouncementCard_1
        )

        val result = subject.reduce(initialState)

        assertEquals(result.assets, initialState.assets)
        assertEquals(result.showAssetSheetFor, initialState.showAssetSheetFor)
        assertEquals(result.announcement, testAnnouncementCard_1)
    }

    @Test
    fun `replacing an asset details type with the same type has no effect`() {
        val initialState = DashboardState(
            assets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            showAssetSheetFor = CryptoCurrency.ETHER,
            announcement = testAnnouncementCard_1
        )

        val result = subject.reduce(initialState)

        assertEquals(result, initialState)
    }
}