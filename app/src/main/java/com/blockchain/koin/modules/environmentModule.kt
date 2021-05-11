package com.blockchain.koin.modules

import com.blockchain.network.EnvironmentUrls
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.data.api.EnvironmentSettings
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

val environmentModule = module {
    single { EnvironmentSettings() }
        .bind(EnvironmentUrls::class)
        .bind(EnvironmentConfig::class)
}
