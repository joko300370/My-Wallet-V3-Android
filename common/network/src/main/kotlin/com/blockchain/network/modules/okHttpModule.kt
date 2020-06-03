package com.blockchain.network.modules

import com.blockchain.network.TLSSocketFactory
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

private const val API_TIMEOUT = 30L
private const val PING_INTERVAL = 10L
val okHttpModule = module {
    single {
        val builder = OkHttpClient.Builder()
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
            .connectTimeout(API_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(API_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(API_TIMEOUT, TimeUnit.SECONDS)
            .pingInterval(PING_INTERVAL, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .certificatePinner(get())

        get<OkHttpInterceptors>().forEach {
            builder.addInterceptor(it)
        }

        /*
          Enable TLS specific version V.1.2
          Issue Details : https://github.com/square/okhttp/issues/1934
         */
        TLSSocketFactory().also {
            builder.sslSocketFactory(it, it.systemDefaultTrustManager())
        }
        builder.build()
    }
}