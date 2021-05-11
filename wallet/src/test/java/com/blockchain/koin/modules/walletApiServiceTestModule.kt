package com.blockchain.koin.modules

import com.blockchain.koin.bigInteger
import com.blockchain.network.EnvironmentUrls
import com.blockchain.network.modules.MoshiBuilderInterceptorList
import com.blockchain.network.modules.OkHttpInterceptors
import info.blockchain.wallet.ApiCode
import io.fabric8.mockwebserver.DefaultMockServer
import okhttp3.OkHttpClient
import org.koin.dsl.bind
import org.koin.dsl.module

fun walletApiServiceTestModule(server: DefaultMockServer) = module {

    single { OkHttpInterceptors(emptyList()) }

    single { OkHttpClient() }

    factory {
        object : ApiCode {
            override val apiCode: String
                get() = "test"
        }
    }.bind(ApiCode::class)

    single {
        MoshiBuilderInterceptorList(
            listOf(get(bigInteger))
        )
    }

    single {
        object : EnvironmentUrls {

            override val apiUrl: String
                get() = server.url("")

            override val everypayHostUrl: String
                get() = throw NotImplementedError()
            override val statusUrl: String
                get() = throw NotImplementedError()
        }
    }.bind(EnvironmentUrls::class)
}