package com.blockchain.nabu.datamanagers.analytics

import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.utils.toUtcIso8601
import info.blockchain.api.AnalyticsService
import info.blockchain.api.NabuAnalyticsEvent
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import java.util.Date

class NabuAnalytics(
    private val analyticsService: AnalyticsService,
    private val prefs: Lazy<PersistentPrefs>
) : Analytics {

    private val id: String by lazy {
        prefs.value.deviceId
    }

    // TODO Add local persistence, instead of log to nabu every single event
    override fun logEvent(analyticsEvent: AnalyticsEvent) {
        analyticsService.postEvent(
            id = id,
            events = listOf(
                analyticsEvent.toNabuAnalyticsEvent()
            ),
            timeStamp = Date().toUtcIso8601()
        )
            .onErrorComplete()
            .emptySubscribe()
    }

    override fun logEventOnce(analyticsEvent: AnalyticsEvent) {}

    override fun logEventOnceForSession(analyticsEvent: AnalyticsEvent) {}
}

private fun AnalyticsEvent.toNabuAnalyticsEvent(): NabuAnalyticsEvent =
    NabuAnalyticsEvent(
        name = this.event,
        type = "EVENT",
        properties = this.params
    )