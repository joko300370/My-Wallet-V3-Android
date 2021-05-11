package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.blockchain.preferences.SimpleBuyPrefs
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder

class IncreaseLimitsAnnouncementTest {
    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()
    private val announcementQueries: AnnouncementQueries = mock()
    private val simpleBuyPrefs: SimpleBuyPrefs = mock()

    private lateinit var subject: IncreaseLimitsAnnouncement

    @Before
    fun setUp() {
        whenever(dismissRecorder[IncreaseLimitsAnnouncement.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(IncreaseLimitsAnnouncement.DISMISS_KEY)

        subject = IncreaseLimitsAnnouncement(dismissRecorder, announcementQueries, simpleBuyPrefs)
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
    fun `should not show, when gold`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(announcementQueries.isGoldComplete()).thenReturn(Single.just(true))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when  not SimplifiedDueDiligence verified`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(announcementQueries.isGoldComplete()).thenReturn(Single.just(false))
        whenever(announcementQueries.isSimplifiedDueDiligenceVerified()).thenReturn(Single.just(false))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not  show, when   SimplifiedDueDiligence verified but no buys happened`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(announcementQueries.isGoldComplete()).thenReturn(Single.just(false))
        whenever(announcementQueries.isSimplifiedDueDiligenceVerified()).thenReturn(Single.just(true))
        whenever(simpleBuyPrefs.hasCompletedAtLeastOneBuy).thenReturn(false)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should   show, when   SimplifiedDueDiligence verified and at least 1 buy happened`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(announcementQueries.isGoldComplete()).thenReturn(Single.just(false))
        whenever(announcementQueries.isSimplifiedDueDiligenceVerified()).thenReturn(Single.just(true))
        whenever(simpleBuyPrefs.hasCompletedAtLeastOneBuy).thenReturn(true)

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }
}