package com.blockchain.koin.modules

import piuk.blockchain.android.BuildConfig

val appProperties = mapOf(
    "app-version" to BuildConfig.VERSION_NAME,
    "os_type" to "android"
)

val keys = mapOf(
    "api-code" to "25a6ad13-1633-4dfb-b6ee-9b91cdf0b5c3",
    "site-key" to BuildConfig.RECAPTCHA_SITE_KEY
)

val urls = mapOf(
    "HorizonURL" to BuildConfig.HORIZON_URL,
    "explorer-api" to BuildConfig.EXPLORER_URL,
    "blockchain-api" to BuildConfig.API_URL
)
