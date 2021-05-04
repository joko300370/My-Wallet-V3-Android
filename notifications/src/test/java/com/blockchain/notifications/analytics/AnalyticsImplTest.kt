package com.blockchain.notifications.analytics

import android.content.SharedPreferences
import com.blockchain.featureflags.GatedFeature
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.google.firebase.analytics.FirebaseAnalytics
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.amshove.kluent.itReturns
import org.junit.Test

class AnalyticsImplTest {

    private val mockFirebase: FirebaseAnalytics = mock()
    private val mockEditor: SharedPreferences.Editor = mock()

    private val event = object : AnalyticsEvent {
        override val event: String
            get() = "name"
        override val params: Map<String, String> = emptyMap()
    }

    private val mockInternalFeatureFlagApi: InternalFeatureFlagApi = mock {
        on { isFeatureEnabled(GatedFeature.SEGMENT_ANALYTICS) } itReturns false
    }

    private val internalFeatureFlagApi: Lazy<InternalFeatureFlagApi> = mock {
        on { value } itReturns mockInternalFeatureFlagApi
    }

    @Test
    fun `should log custom event`() {
        val mockStore = mock<SharedPreferences>()

        AnalyticsImpl(
            firebaseAnalytics = mockFirebase, store = mockStore,
            internalFeatureFlagApi = internalFeatureFlagApi,
            nabuAnalytics = mock()
        ).logEvent(event)

        verify(mockFirebase).logEvent(event.event, null)
    }

    @Test
    fun `should log once event once`() {
        val mockStore = mock<SharedPreferences> {
            on { contains(any()) } doReturn false
            on { edit() } doReturn mockEditor
        }

        whenever(mockEditor.putBoolean(any(), any())).thenReturn(mockEditor)

        AnalyticsImpl(
            firebaseAnalytics = mockFirebase,
            store = mockStore,
            internalFeatureFlagApi = internalFeatureFlagApi,
            nabuAnalytics = mock()
        ).logEventOnce(event)
        verify(mockFirebase).logEvent(event.event, null)
    }

    @Test
    fun `should not log once event again`() {
        val mockStore = mock<SharedPreferences> {
            on { contains(any()) } doReturn true
            on { edit() } doReturn mockEditor
        }

        AnalyticsImpl(
            firebaseAnalytics = mockFirebase,
            store = mockStore,
            internalFeatureFlagApi = internalFeatureFlagApi,
            nabuAnalytics = mock()
        ).logEventOnce(event)
        verify(mockFirebase, never()).logEvent(event.event, null)
    }
}