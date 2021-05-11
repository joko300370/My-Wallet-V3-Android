package piuk.blockchain.androidcore.data.api.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.utils.PersistentPrefs

class DeviceIdInterceptor(private val prefs: Lazy<PersistentPrefs>, private val environmentConfig: EnvironmentConfig) :
    Interceptor {
    private var deviceId: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        if (chain.request().url.toUrl().toString().contains(environmentConfig.nabuApi)) {
            val requestWithUserAgent = originalRequest.newBuilder()
                .header("X-DEVICE-ID", deviceId ?: prefs.value.deviceId.also {
                    deviceId = it
                }).build()
            return chain.proceed(requestWithUserAgent)
        }
        return chain.proceed(originalRequest)
    }
}