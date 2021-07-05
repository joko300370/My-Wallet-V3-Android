package com.blockchain.api.util

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.IOException

class MockInterceptor : Interceptor {
    var responseString = ""
    var responseCode = 200
    var ioException = false

    override fun intercept(chain: Interceptor.Chain): Response {
        if (ioException)
            throw IOException()

        return Response.Builder()
            .code(responseCode)
            .message(responseString)
            .request(chain.request())
            .protocol(Protocol.HTTP_1_0)
            .body(
                ResponseBody.create(
                    "application/json".toMediaType(),
                    responseString.toByteArray()
                )
            )
            .addHeader("content-type", "application/json")
            .build()
    }
}