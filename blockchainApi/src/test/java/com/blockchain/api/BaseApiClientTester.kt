package com.blockchain.api

import com.blockchain.api.util.MockInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory

const val API_CODE = "12345"

abstract class BaseApiClientTester {
    @JvmField
    protected val mockInterceptor = MockInterceptor()

    private fun makeOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(mockInterceptor)
            .build()
    }

    protected fun makeRetrofitApi(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(API_URL)
            .client(makeOkHttpClient())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }.asConverterFactory("application/json".toMediaType())
            )
            .build()
    }

    companion object {
        private const val API_URL = "https://api.blockchain.info"
    }
}