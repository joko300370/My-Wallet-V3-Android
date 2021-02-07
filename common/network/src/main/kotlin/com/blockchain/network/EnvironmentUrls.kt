package com.blockchain.network

interface EnvironmentUrls {
    val explorerUrl: String
    val apiUrl: String
    val everypayHostUrl: String

    val nabuApi: String
        get() = "${apiUrl}nabu-gateway/"
}
