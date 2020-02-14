package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.blockchain.preferences.WalletStatus
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.simplebuy.SimpleBuyAvailability
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager

class BuyBitcoinAnnouncementTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()
    private val walletStatus: WalletStatus = mock()
    private val buyDataManager: BuyDataManager = mock()
    private val simpleBuyAvailability: SimpleBuyAvailability = mock()

    private lateinit var subject: BuyBitcoinAnnouncement

    @Before
    fun setUp() {
        whenever(dismissRecorder[BuyBitcoinAnnouncement.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(BuyBitcoinAnnouncement.DISMISS_KEY)
        whenever(simpleBuyAvailability.isAvailable()).thenReturn(Single.just(false))

        subject =
            BuyBitcoinAnnouncement(
                dismissRecorder = dismissRecorder,
                walletStatus = walletStatus,
                buyDataManager = buyDataManager,
                simpleBuyAvailability = simpleBuyAvailability
            )
    }

    @Test
    fun `should not show, when already shown`() {
        whenever(dismissEntry.isDismissed).thenReturn(true)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should show, when not already shown, wallet is unfunded and buy is allowed`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(walletStatus.isWalletFunded).thenReturn(true)

        whenever(walletStatus.isWalletFunded).thenReturn(false)
        whenever(buyDataManager.canBuy).thenReturn(Single.just(true))

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when not already shown, wallet is funded and buy is allowed`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(walletStatus.isWalletFunded).thenReturn(true)

        whenever(walletStatus.isWalletFunded).thenReturn(true)
        whenever(buyDataManager.canBuy).thenReturn(Single.just(true))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should show, when not already shown, wallet is unfunded and buy is not allowed`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(walletStatus.isWalletFunded).thenReturn(true)

        whenever(walletStatus.isWalletFunded).thenReturn(false)
        whenever(buyDataManager.canBuy).thenReturn(Single.just(false))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when simplebuy available`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(walletStatus.isWalletFunded).thenReturn(true)

        whenever(walletStatus.isWalletFunded).thenReturn(false)
        whenever(buyDataManager.canBuy).thenReturn(Single.just(true))
        whenever(simpleBuyAvailability.isAvailable()).thenReturn(Single.just(true))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }
}
