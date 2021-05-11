package info.blockchain.api.analytics

import info.blockchain.api.NabuAnalyticsEvent
import io.reactivex.Completable
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

interface AnalyticsApiInterface {
    @POST("events/publish")
    fun postAnalytics(
        @Body body: AnalyticsRequestBody
    ): Completable
}

@Serializable
class AnalyticsRequestBody(
    val id: String,
    val context: AnalyticsContext = AnalyticsContext(),
    val events: List<NabuAnalyticsEvent>
)

@Serializable
class AnalyticsContext(val context: Map<String, String> = mapOf())
