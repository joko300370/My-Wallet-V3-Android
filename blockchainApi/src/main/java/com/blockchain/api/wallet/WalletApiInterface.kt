package com.blockchain.api.wallet

import com.blockchain.api.wallet.data.WalletSettingsDto
import io.reactivex.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

internal interface WalletApiInterface {

    @FormUrlEncoded
    @POST("wallet")
    fun fetchSettings(
        @Field("guid") guid: String,
        @Field("sharedKey") sharedKey: String,
        @Field("api_code") apiCode: String,
        @Field("method") method: String = METHOD_GET_INFO,
        @Field("format") format: String = "plain"
    ): Single<WalletSettingsDto>

    companion object {
        internal const val METHOD_GET_INFO = "get-info"
    }
}