package com.blockchain.swap.nabu.models.responses.nabu

import com.blockchain.serialization.JsonSerializable

data class VeriffToken(
    val applicantId: String,
    val token: String,
    val data: Data
) : JsonSerializable

data class Data(
    val url: String
) : JsonSerializable
