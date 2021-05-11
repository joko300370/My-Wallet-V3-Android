package com.blockchain.notifications.analytics

import android.content.SharedPreferences
import android.os.Bundle
import com.blockchain.featureflags.GatedFeature
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.google.firebase.analytics.FirebaseAnalytics

class AnalyticsImpl internal constructor(
    private val firebaseAnalytics: FirebaseAnalytics,
    private val nabuAnalytics: Analytics,
    private val store: SharedPreferences,
    private val internalFeatureFlagApi: Lazy<InternalFeatureFlagApi>
) : Analytics {

    private val sentAnalytics = mutableSetOf<String>()

    override fun logEvent(analyticsEvent: AnalyticsEvent) {
        firebaseAnalytics.logEvent(analyticsEvent.event, toBundle(analyticsEvent.params))
        if (internalFeatureFlagApi.value.isFeatureEnabled(GatedFeature.SEGMENT_ANALYTICS)) {
            nabuAnalytics.logEvent(analyticsEvent)
        }
    }

    override fun logEventOnce(analyticsEvent: AnalyticsEvent) {
        if (!hasSentMetric(analyticsEvent.event)) {
            setMetricAsSent(analyticsEvent.event)
            logEvent(analyticsEvent)
        }
    }

    override fun logEventOnceForSession(analyticsEvent: AnalyticsEvent) {
        if (!sentAnalytics.contains(analyticsEvent.event)) {
            logEvent(analyticsEvent)
            sentAnalytics.add(analyticsEvent.event)
        }
    }

    private fun toBundle(params: Map<String, String>): Bundle? {
        if (params.isEmpty()) return null

        return Bundle().apply {
            params.forEach { (k, v) -> putString(k, v) }
        }
    }

    private fun hasSentMetric(metricName: String) =
        store.contains("HAS_SENT_METRIC_$metricName")

    private fun setMetricAsSent(metricName: String) =
        store.edit().putBoolean("HAS_SENT_METRIC_$metricName", true).apply()
}