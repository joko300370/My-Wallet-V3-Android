package com.blockchain.network

import info.blockchain.balance.CryptoCurrency

interface EnvironmentUrls {
    val explorerUrl: String
    val apiUrl: String
    val everypayHostUrl: String

    fun websocketUrl(currency: CryptoCurrency): String

    val nabuApi: String
        get() = "${apiUrl}nabu-gateway/"
}
