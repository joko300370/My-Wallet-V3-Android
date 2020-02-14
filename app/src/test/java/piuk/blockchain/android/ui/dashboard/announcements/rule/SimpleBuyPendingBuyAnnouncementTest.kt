package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.blockchain.notifications.analytics.Analytics
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder

class SimpleBuyPendingBuyAnnouncementTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()

    private lateinit var subject: SimpleBuyPendingBuyAnnouncement

    private val analytics: Analytics = mock()
    private val queries: AnnouncementQueries = mock()

    @Before
    fun setUp() {
        whenever(dismissRecorder[SimpleBuyPendingBuyAnnouncement.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(SimpleBuyPendingBuyAnnouncement.DISMISS_KEY)

        subject =
            SimpleBuyPendingBuyAnnouncement(
                dismissRecorder = dismissRecorder,
                analytics = analytics,
                queries = queries
            )
    }

    @Test
    fun `should ignore dismiss recorder and show when isDismissed is set and buy is pending`() {
        whenever(dismissEntry.isDismissed).thenReturn(true)
        whenever(queries.isSimpleBuyTransactionPending()).thenReturn(Single.just(true))

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should ignore dismiss recorder and show when isDismissed is unset and buy is pending`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(queries.isSimpleBuyTransactionPending()).thenReturn(Single.just(true))

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }
}