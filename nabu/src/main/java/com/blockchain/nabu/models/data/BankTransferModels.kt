package com.blockchain.nabu.models.data

import com.blockchain.nabu.models.responses.banktransfer.LinkBankAttrsResponse

data class LinkBankTransfer(val id: String, val partner: BankPartner, val attributes: LinkBankAttributes)

enum class BankPartner {
    YODLEE;

    fun attributes(attrsResponse: LinkBankAttrsResponse): LinkBankAttributes =
        when (this) {
            YODLEE -> {
                require(attrsResponse.fastlinkUrl != null)
                require(attrsResponse.token != null)
                YodleeAttributes(attrsResponse.fastlinkUrl, attrsResponse.token)
            }
        }
}

interface LinkBankAttributes

class YodleeAttributes(val fastlinkUrl: String, val token: String) : LinkBankAttributes

data class LinkedBank(
    val id: String,
    val currency: String,
    val partner: BankPartner,
    val name: String,
    val accountNumber: String,
    val state: LinkedBankState,
    val errorStatus: LinkedBankErrorState
)

enum class LinkedBankErrorState {
    ACCOUNT_ALREADY_LINKED,
    ACCOUNT_TYPE_UNSUPPORTED,
    UNKNOWN,
    NONE
}

enum class LinkedBankState {
    PENDING, BLOCKED, ACTIVE, UNKNOWN
}
