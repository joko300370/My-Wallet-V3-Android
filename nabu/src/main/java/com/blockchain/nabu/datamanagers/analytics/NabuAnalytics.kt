package com.blockchain.nabu.datamanagers.analytics

import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvent
import info.blockchain.api.AnalyticsService
import info.blockchain.api.NabuAnalyticsEvent
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe

class NabuAnalytics(
    private val analyticsService: AnalyticsService,
    private val prefs: Lazy<PersistentPrefs>,
    private val localAnalyticsPersistence: AnalyticsLocalPersistence
) : Analytics {
    private val compositeDisposable = CompositeDisposable()

    private val id: String by lazy {
        prefs.value.deviceId
    }

    override fun logEvent(analyticsEvent: AnalyticsEvent) {
        val nabuEvent = analyticsEvent.toNabuAnalyticsEvent()
        // TODO log failure
        compositeDisposable += localAnalyticsPersistence.save(nabuEvent)
            .subscribeOn(Schedulers.computation())
            .onErrorComplete()
            .emptySubscribe()
        // TODO check condition and batch to the api if condition is met
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