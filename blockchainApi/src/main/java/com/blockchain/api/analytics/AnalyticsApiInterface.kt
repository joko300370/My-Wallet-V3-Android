package com.blockchain.api.analytics

import com.blockchain.api.NabuAnalyticsEvent
import io.reactivex.Completable
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AnalyticsApiInterface {
    @POST("events/publish")
    fun postAnalytics(
        @Header("authorization") authorization: String?,
        @Body body: AnalyticsRequestBody
    ): Completable
}

@Serializable
class AnalyticsRequestBody(
    val id: String,
    val context: AnalyticsContext,
    val platform: String,
    val events: List<NabuAnalyticsEvent>
)

@Serializable
class AnalyticsContext(val device: DeviceInfo, val locale: String, val screen: ScreenInfo, val timezone: String)

@Serializable
class DeviceInfo(val manufacturer: String?, val model: String, val name: String)

@Serializable
class ScreenInfo(val density: Float, val height: Int, val width: Int)