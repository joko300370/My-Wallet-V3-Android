package info.blockchain.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.reactivex.schedulers.Schedulers
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import org.koin.core.qualifier.StringQualifier
import org.koin.core.scope.Scope
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory

val blockchainApi = StringQualifier("blockchain-api")
val explorerApi = StringQualifier("explorer-api")

val blockchainApiModule = module {

    single { RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()) }

    single(blockchainApi) {
        Retrofit.Builder()
            .baseUrl(getBaseUrl("blockchain-api"))
            .client(get())
            .addCallAdapterFactory(get<RxJava2CallAdapterFactory>())
            .addConverterFactory(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }.asConverterFactory("application/json".toMediaType())
            )
            .build()
    }

    single(explorerApi) {
        Retrofit.Builder()
            .baseUrl(getBaseUrl("explorer-api"))
            .client(get())
            .addCallAdapterFactory(get<RxJava2CallAdapterFactory>())
            .addConverterFactory(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }.asConverterFactory("application/json".toMediaType())
            )
            .build()
    }

    factory {
        BitcoinApi(
            get(blockchainApi),
            getProperty("api-code")
        )
    }

    factory {
        WalletSettingsService(
            get(explorerApi),
            getProperty("api-code")
        )
    }
    factory {
        AnalyticsService(
            get(blockchainApi)
        )
    }
}

fun Scope.getBaseUrl(propName: String): String = getProperty(propName)
