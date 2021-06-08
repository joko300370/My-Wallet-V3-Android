package com.blockchain.nabu.datamanagers.analytics

import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.stores.NabuSessionTokenStore
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.LaunchOrigin
import com.blockchain.operations.AppStartUpFlushable
import com.blockchain.utils.Optional
import com.blockchain.utils.toUtcIso8601
import info.blockchain.api.AnalyticsService
import info.blockchain.api.NabuAnalyticsEvent
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import piuk.blockchain.androidcore.utils.extensions.then
import java.math.BigDecimal
import java.util.Date
import java.util.Locale

class NabuAnalytics(
    private val analyticsService: AnalyticsService,
    private val prefs: Lazy<PersistentPrefs>,
    private val localAnalyticsPersistence: AnalyticsLocalPersistence,
    private val crashLogger: CrashLogger,
    private val analyticsContextProvider: AnalyticsContextProvider,
    private val tokenStore: NabuSessionTokenStore
) : Analytics, AppStartUpFlushable {
    private val compositeDisposable = CompositeDisposable()

    private val id: String by lazy {
        prefs.value.deviceId
    }

    override fun logEvent(analyticsEvent: AnalyticsEvent) {
        val nabuEvent = analyticsEvent.toNabuAnalyticsEvent()

        compositeDisposable += localAnalyticsPersistence.save(nabuEvent)
            .subscribeOn(Schedulers.computation())
            .doOnError {
                crashLogger.logException(it)
            }
            .onErrorComplete()
            .then {
                sendToApiAndFlushIfNeeded()
            }
            .emptySubscribe()
    }

    private fun sendToApiAndFlushIfNeeded(): Completable {
        return localAnalyticsPersistence.size().flatMapCompletable {
            if (it >= BATCH_SIZE) {
                batchToApiAndFlush()
            } else {
                Completable.complete()
            }
        }
    }

    override val tag: String
        get() = "nabu_analytics_flush"

    override fun flush(): Completable {
        return localAnalyticsPersistence.getAllItems().flatMapCompletable { events ->
            // Whats happening here is that we split the retrieved items into sublists of size = BATCH_SIZE
            // and then each one of these sublists is converted to the corresponding completable that actually is the
            // api request.

            val listOfSublists = mutableListOf<List<NabuAnalyticsEvent>>()
            for (i in events.indices step BATCH_SIZE) {
                listOfSublists.add(
                    events.subList(i, (i + BATCH_SIZE).coerceAtMost(events.size))
                )
            }

            val completables = listOfSublists.map { list ->
                postEvents(list).then {
                    localAnalyticsPersistence.removeOldestItems(list.size)
                }
            }
            Completable.concat(completables)
        }
    }

    private fun batchToApiAndFlush(): Completable {
        return localAnalyticsPersistence.getOldestItems(BATCH_SIZE).flatMapCompletable {
            postEvents(it)
        }.then {
            localAnalyticsPersistence.removeOldestItems(BATCH_SIZE)
        }
    }

    private fun postEvents(events: List<NabuAnalyticsEvent>): Completable =
        tokenStore.getAccessToken().firstOrError().flatMapCompletable {
            analyticsService.postEvents(
                events = events,
                id = id,
                analyticsContext = analyticsContextProvider.context(),
                platform = "WALLET",
                authorization = if (it is Optional.Some) it.element.authHeader else null
            )
        }

    override fun logEventOnce(analyticsEvent: AnalyticsEvent) {}

    override fun logEventOnceForSession(analyticsEvent: AnalyticsEvent) {}

    companion object {
        private const val BATCH_SIZE = 10
    }
}

private fun AnalyticsEvent.toNabuAnalyticsEvent(): NabuAnalyticsEvent =
    NabuAnalyticsEvent(
        name = this.event,
        type = "EVENT",
        originalTimestamp = Date().toUtcIso8601(Locale.ENGLISH),
        properties = this.params.filter { it.value is String }.mapValues { it.value.toString() }
            .plusOriginIfAvailable(this.origin),
        numericProperties = this.params.filter { it.value is Number }.mapValues { BigDecimal(it.value.toString()) }
    )

private fun Map<String, String>.plusOriginIfAvailable(launchOrigin: LaunchOrigin?): Map<String, String> {
    val origin = launchOrigin ?: return this
    return this.toMutableMap().apply {
        this["origin"] = origin.name
    }.toMap()
}
