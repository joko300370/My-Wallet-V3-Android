package info.blockchain.api

import info.blockchain.api.analytics.AnalyticsApiInterface
import info.blockchain.api.analytics.AnalyticsContext
import info.blockchain.api.analytics.AnalyticsRequestBody
import io.reactivex.Completable
import kotlinx.serialization.Serializable
import retrofit2.Retrofit

class AnalyticsService(retrofit: Retrofit) {
    private val api: AnalyticsApiInterface = retrofit.create(AnalyticsApiInterface::class.java)

    fun postEvents(events: List<NabuAnalyticsEvent>, id: String): Completable {
        return api.postAnalytics(
            AnalyticsRequestBody(
                id = id,
                events = events,
                context = AnalyticsContext()
            )
        )
    }
}

@Serializable
data class NabuAnalyticsEvent(
    val name: String,
    val type: String,
    val originalTimestamp: String,
    val properties: Map<String, String> = emptyMap()
)
