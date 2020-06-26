package com.blockchain.koin.modules

import com.blockchain.koin.nabu
import com.blockchain.network.websocket.Options
import org.koin.dsl.module
import piuk.blockchain.android.BuildConfig

val nabuUrlModule = module {

    single(nabu) {
        Options(
            name = "Nabu",
            url = BuildConfig.NABU_WEBSOCKET_URL
        )
    }
}
