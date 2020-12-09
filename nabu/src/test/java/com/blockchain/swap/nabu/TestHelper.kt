package com.blockchain.swap.nabu

import com.blockchain.swap.nabu.models.responses.nabu.KycState
import com.blockchain.swap.nabu.models.responses.nabu.NabuUser
import com.blockchain.swap.nabu.models.responses.nabu.UserState
import com.blockchain.swap.nabu.metadata.NabuCredentialsMetadata
import com.blockchain.swap.nabu.models.responses.tokenresponse.NabuOfflineTokenResponse

fun getBlankNabuUser(kycState: KycState = KycState.None): NabuUser = NabuUser(
    firstName = "",
    lastName = "",
    email = "",
    emailVerified = false,
    dob = null,
    mobile = "",
    mobileVerified = false,
    address = null,
    state = UserState.None,
    kycState = kycState,
    insertedAt = "",
    updatedAt = ""
)

val validOfflineTokenMetadata get() = NabuCredentialsMetadata("userId", "lifetimeToken")
val validOfflineToken get() = NabuOfflineTokenResponse("userId",
    "lifetimeToken")