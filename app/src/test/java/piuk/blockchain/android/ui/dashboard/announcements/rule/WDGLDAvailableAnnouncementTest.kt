package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder

class WDGLDAvailableAnnouncementTest {
    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()

    private lateinit var subject: WDGLDAvailableAnnouncement

    @Before
    fun setUp() {
        whenever(dismissRecorder[WDGLDAvailableAnnouncement.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(WDGLDAvailableAnnouncement.DISMISS_KEY)

        subject =
            WDGLDAvailableAnnouncement(
                dismissRecorder = dismissRecorder
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
    fun `should show, when not already shown`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }
}
