package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.DashboardPrefs
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test

import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder

class IntroTourAnnouncementTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()
    private val prefs: DashboardPrefs = mock()
    private val analytics: Analytics = mock()

    private lateinit var subject: IntroTourAnnouncement

    @Before
    fun setUp() {
        whenever(dismissRecorder[IntroTourAnnouncement.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(IntroTourAnnouncement.DISMISS_KEY)

        subject =
            IntroTourAnnouncement(
                dismissRecorder = dismissRecorder,
                prefs = prefs,
                analytics = analytics
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
    fun `should show, when not already shown and the tour has not been completed`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(prefs.isTourComplete).thenReturn(false)

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when not already shown and the tour has been completed`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(prefs.isTourComplete).thenReturn(true)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }
}
