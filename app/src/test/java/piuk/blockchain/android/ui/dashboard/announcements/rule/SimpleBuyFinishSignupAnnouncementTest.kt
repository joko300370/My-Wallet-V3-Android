package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.blockchain.notifications.analytics.Analytics
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder

class SimpleBuyFinishSignupAnnouncementTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()

    private lateinit var subject: SimpleBuyFinishSignupAnnouncement

    private val analytics: Analytics = mock()
    private val queries: AnnouncementQueries = mock()

    @Before
    fun setUp() {
        whenever(dismissRecorder[SimpleBuyFinishSignupAnnouncement.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(SimpleBuyFinishSignupAnnouncement.DISMISS_KEY)

        subject =
            SimpleBuyFinishSignupAnnouncement(
                dismissRecorder = dismissRecorder,
                analytics = analytics,
                queries = queries
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
    fun `should show, when not already shown and simple buy kyc is incomplete`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(queries.isSimpleBuyKycInProgress()).thenReturn(Single.just(true))

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when not already shown and simple buy kyc is complete`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(queries.isSimpleBuyKycInProgress()).thenReturn(Single.just(false))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }
}