package com.blockchain.api.addressmapping.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddressMapRequest(
    @SerialName("name")
    val domainName: String, // Domain name to resolve
    @SerialName("currency")
    val assetTicker: String // Which currency to lookup
)

@Serializable
data class AddressMapResponse(
    @SerialName("currency")
    val assetTicker: String,
    @SerialName("address")
    val address: String
)
