package com.blockchain.swap.nabu.models.responses.tokenresponse

import com.blockchain.swap.nabu.metadata.NabuCredentialsMetadata

data class NabuOfflineTokenRequest(
    val jwt: String
)

data class NabuOfflineTokenResponse(
    val userId: String,
    val token: String
)

fun NabuOfflineTokenResponse.mapToMetadata(): NabuCredentialsMetadata =
    NabuCredentialsMetadata(this.userId, this.token)

fun NabuCredentialsMetadata.mapFromMetadata(): NabuOfflineTokenResponse =
    NabuOfflineTokenResponse(this.userId, this.lifetimeToken)
