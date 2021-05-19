@file:UseSerializers(BigDecimalSerializer::class)
package info.blockchain.api

import info.blockchain.api.analytics.AnalyticsApiInterface
import info.blockchain.api.analytics.AnalyticsContext
import info.blockchain.api.analytics.AnalyticsRequestBody
import info.blockchain.api.serializers.BigDecimalSerializer
import io.reactivex.Completable
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import retrofit2.Retrofit
import java.math.BigDecimal

class AnalyticsService(retrofit: Retrofit) {
    private val api: AnalyticsApiInterface = retrofit.create(AnalyticsApiInterface::class.java)

    fun postEvents(
        events: List<NabuAnalyticsEvent>,
        id: String,
        analyticsContext: AnalyticsContext,
        authorization: String?
    ): Completable {

        return api.postAnalytics(
            authorization,
            AnalyticsRequestBody(
                id = id,
                events = events,
                context = analyticsContext
            )
        )
    }
}

@Serializable
data class NabuAnalyticsEvent(
    val name: String,
    val type: String,
    val originalTimestamp: String,
    val properties: Map<String, String>,
    val numericProperties: Map<String, BigDecimal>
)
