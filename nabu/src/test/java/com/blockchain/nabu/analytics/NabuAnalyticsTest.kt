package com.blockchain.nabu.analytics

import com.blockchain.nabu.datamanagers.analytics.AnalyticsContextProvider
import com.blockchain.nabu.datamanagers.analytics.AnalyticsLocalPersistence
import com.blockchain.nabu.datamanagers.analytics.NabuAnalytics
import com.blockchain.nabu.models.responses.tokenresponse.NabuSessionTokenResponse
import com.blockchain.nabu.stores.NabuSessionTokenStore
import com.blockchain.utils.Optional
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.whenever
import com.blockchain.api.AnalyticsService
import com.blockchain.api.NabuAnalyticsEvent
import com.blockchain.api.analytics.AnalyticsContext
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.any
import org.amshove.kluent.itReturns
import org.amshove.kluent.mock
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.androidcore.utils.PersistentPrefs

class NabuAnalyticsTest {
    private val localAnalyticsPersistence = mock<AnalyticsLocalPersistence>()

    private val token: Optional<NabuSessionTokenResponse> = Optional.Some(
        NabuSessionTokenResponse(
            "", "", "", true, "", "", ""
        )
    )
    private val tokenStore: NabuSessionTokenStore = mock {
        on { getAccessToken() } itReturns Observable.just(token)
    }

    private val persistentPrefs: PersistentPrefs = mock {
        on { deviceId } itReturns "deviceID"
    }
    private val prefs: Lazy<PersistentPrefs> = mock {
        onGeneric { value } itReturns persistentPrefs
    }
    private val mockedContext: AnalyticsContext = mock()

    private val analyticsService = mock<AnalyticsService>()

    private val analyticsContextProvider: AnalyticsContextProvider = mock {
        on { context() } itReturns mockedContext
    }

    private val nabuAnalytics =
        NabuAnalytics(
            localAnalyticsPersistence = localAnalyticsPersistence, prefs = prefs,
            crashLogger = mock(), analyticsService = analyticsService, tokenStore = tokenStore,
            analyticsContextProvider = analyticsContextProvider
        )

    @Test
    fun flushIsWorking() {
        whenever(analyticsService.postEvents(any(), any(), any(), any(), any())).thenReturn(Completable.complete())
        whenever(localAnalyticsPersistence.getAllItems()).thenReturn(Single.just(randomListOfEventsWithSize(84)))
        whenever(localAnalyticsPersistence.removeOldestItems(any())).thenReturn(Completable.complete())
        val testSubscriber = nabuAnalytics.flush().test()

        testSubscriber.assertComplete()
        Mockito.verify(analyticsService, times(9)).postEvents(any(), any(), any(), any(), any())

        Mockito.verify(localAnalyticsPersistence, times(8)).removeOldestItems(10)
        Mockito.verify(localAnalyticsPersistence).removeOldestItems(4)
    }

    @Test
    fun flushOnEmptyStorageShouldNotInvokeAnyPosts() {
        whenever(analyticsService.postEvents(any(), any(), any(), any(), any())).thenReturn(Completable.complete())
        whenever(localAnalyticsPersistence.getAllItems()).thenReturn(Single.just(randomListOfEventsWithSize(0)))
        whenever(localAnalyticsPersistence.removeOldestItems(any())).thenReturn(Completable.complete())
        val testSubscriber = nabuAnalytics.flush().test()

        testSubscriber.assertComplete()
        Mockito.verify(analyticsService, never()).postEvents(any(), any(), any(), any(), any())

        Mockito.verify(localAnalyticsPersistence, never()).removeOldestItems(any())
    }

    @Test
    fun ifPostFailsCompletableShouldFailToo() {
        whenever(analyticsService.postEvents(any(), any(), any(), any(), any())).thenReturn(
            Completable.error(Throwable())
        )
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
                originalTimestamp = "originalTimestamp$it",
                properties = emptyMap(),
                numericProperties = emptyMap()
            )
        }
    }
}