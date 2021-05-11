package com.blockchain.nabu.analytics

import com.blockchain.nabu.datamanagers.analytics.AnalyticsLocalPersistence
import com.blockchain.nabu.datamanagers.analytics.NabuAnalytics
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.api.AnalyticsService
import info.blockchain.api.NabuAnalyticsEvent
import io.reactivex.Completable
import io.reactivex.Single
import org.amshove.kluent.any
import org.amshove.kluent.itReturns
import org.amshove.kluent.mock
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.androidcore.utils.PersistentPrefs

class NabuAnalyticsTest {
    private val localAnalyticsPersistence = mock<AnalyticsLocalPersistence>()

    private val persistentPrefs: PersistentPrefs = mock {
        on { deviceId } itReturns "deviceID"
    }
    private val prefs: Lazy<PersistentPrefs> = mock {
        onGeneric { value } itReturns persistentPrefs
    }

    private val analyticsService = mock<AnalyticsService>()

    private val nabuAnalytics =
        NabuAnalytics(
            localAnalyticsPersistence = localAnalyticsPersistence, prefs = prefs, analyticsService = analyticsService
        )

    @Test
    fun flushIsWorking() {
        whenever(analyticsService.postEvents(any(), any())).thenReturn(Completable.complete())
        whenever(localAnalyticsPersistence.getAllItems()).thenReturn(Single.just(randomListOfEventsWithSize(84)))
        whenever(localAnalyticsPersistence.removeOldestItems(any())).thenReturn(Completable.complete())
        val testSubscriber = nabuAnalytics.flush().test()

        testSubscriber.assertComplete()
        Mockito.verify(analyticsService, times(9)).postEvents(any(), any())

        Mockito.verify(localAnalyticsPersistence, times(8)).removeOldestItems(10)
        Mockito.verify(localAnalyticsPersistence).removeOldestItems(4)
    }

    @Test
    fun flushOnEmptyStorageShouldNotInvokeAnyPosts() {
        whenever(analyticsService.postEvents(any(), any())).thenReturn(Completable.complete())
        whenever(localAnalyticsPersistence.getAllItems()).thenReturn(Single.just(randomListOfEventsWithSize(0)))
        whenever(localAnalyticsPersistence.removeOldestItems(any())).thenReturn(Completable.complete())
        val testSubscriber = nabuAnalytics.flush().test()

        testSubscriber.assertComplete()
        Mockito.verify(analyticsService, never()).postEvents(any(), any())

        Mockito.verify(localAnalyticsPersistence, never()).removeOldestItems(any())
    }

    @Test
    fun ifPostFailsCompletableShouldFailToo() {
        whenever(analyticsService.postEvents(any(), any())).thenReturn(Completable.error(Throwable()))
        whenever(localAnalyticsPersistence.getAllItems()).thenReturn(Single.just(randomListOfEventsWithSize(10)))
        whenever(localAnalyticsPersistence.removeOldestItems(any())).thenReturn(Completable.complete())
        val testSubscriber = nabuAnalytics.flush().test()

        testSubscriber.assertNotComplete()
        assert(testSubscriber.errorCount() == 1)
    }

    private fun randomListOfEventsWithSize(i: Int): List<NabuAnalyticsEvent> {
        return IntArray(i) { i }.map {
            NabuAnalyticsEvent(
                name = "name$it",
                type = "type$it",
                originalTimestamp = "originalTimestamp$it"
            )
        }
    }
}