package com.blockchain.nabu.models.data

import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.responses.banktransfer.LinkBankAttrsResponse
import com.blockchain.nabu.models.responses.banktransfer.YapilyMediaResponse
import info.blockchain.balance.FiatValue
import om.blockchain.swap.nabu.BuildConfig
import java.io.Serializable
import java.math.BigInteger
import java.net.MalformedURLException
import java.net.URL

data class LinkBankTransfer(val id: String, val partner: BankPartner, val attributes: LinkBankAttributes) : Serializable

enum class BankPartner {
    YAPILY,
    YODLEE;

    fun attributes(attrsResponse: LinkBankAttrsResponse): LinkBankAttributes =
        when (this) {
            YODLEE -> {
                require(attrsResponse.fastlinkUrl != null)
                require(attrsResponse.token != null)
                require(attrsResponse.fastlinkParams != null)
                YodleeAttributes(
                    attrsResponse.fastlinkUrl, attrsResponse.token,
                    attrsResponse.fastlinkParams.configName
                )
            }
            YAPILY -> {
                require(attrsResponse.institutions != null)
                require(attrsResponse.entity != null)
                YapilyAttributes(
                    entity = attrsResponse.entity,
                    institutionList = attrsResponse.institutions.map {
                        YapilyInstitution(
                            operatingCountries = it.countries.map { countryResponse ->
                                InstitutionCountry(
                                    countryCode = countryResponse.countryCode2,
                                    displayName = countryResponse.displayName
                                )
                            },
                            name = it.fullName,
                            id = it.id,
                            iconLink = it.media.getBankIcon()
                        )
                    }
                )
            }
        }

    private fun List<YapilyMediaResponse>.getBankIcon(): URL? =
        try {
            URL(find { it.type == ICON }?.source)
        } catch (e: MalformedURLException) {
            null
        }

    companion object {
        private const val ICON = "icon"

        // we need to provide this when linking a bank so we can get a deep link back to the app
        // provide the short URL from the firebase dynamic link so it can get mapped against the listing
        const val YAPILY_DEEPLINK_BANK_LINK_URL = "https://${BuildConfig.DEEP_LINK_HOST}/oblinking"

        const val YAPILY_DEEPLINK_PAYMENT_APPROVAL_URL = "https://${BuildConfig.DEEP_LINK_HOST}/obapproval"
    }
}

interface LinkBankAttributes

data class YodleeAttributes(val fastlinkUrl: String, val token: String, val configName: String) : LinkBankAttributes,
    Serializable

data class YapilyAttributes(
    val entity: String,
    val institutionList: List<YapilyInstitution>
) : LinkBankAttributes, Serializable

data class YapilyInstitution(
    val operatingCountries: List<InstitutionCountry>,
    val name: String,
    val id: String,
    val iconLink: URL?
) : Serializable

data class InstitutionCountry(val countryCode: String, val displayName: String) : Serializable

data class LinkedBank(
    val id: String,
    val currency: String,
    val partner: BankPartner,
    val bankName: String,
    val accountName: String,
    val accountNumber: String,
    val state: LinkedBankState,
    val errorStatus: LinkedBankErrorState,
    val accountType: String,
    val authorisationUrl: String,
    val sortCode: String,
    val accountIban: String,
    val bic: String,
    val entity: String,
    val iconUrl: String,
    val callbackPath: String
) : Serializable {
    val account: String
        get() = accountNumber

    val paymentMethod: PaymentMethodType
        get() = PaymentMethodType.BANK_TRANSFER

    fun isLinkingPending() = !isLinkingInFinishedState()

    fun isLinkingInFinishedState() =
        state == LinkedBankState.ACTIVE || state == LinkedBankState.BLOCKED
}

enum class LinkedBankErrorState {
    ACCOUNT_ALREADY_LINKED,
    NAMES_MISMATCHED,
    ACCOUNT_TYPE_UNSUPPORTED,
    REJECTED,
    EXPIRED,
    FAILURE,
    INVALID,
    UNKNOWN,
    NONE
}

enum class LinkedBankState {
    CREATED,
    PENDING,
    BLOCKED,
    ACTIVE,
    UNKNOWN
}

data class FiatWithdrawalFeeAndLimit(
    val minLimit: FiatValue,
    val fee: FiatValue
)

data class CryptoWithdrawalFeeAndLimit(
    val minLimit: BigInteger,
    val fee: BigInteger
)

data class BankTransferDetails(
    val id: String,
    val amount: FiatValue,
    val authorisationUrl: String?,
    val status: BankTransferStatus
)

enum class BankTransferStatus {
    UNKNOWN,
    PENDING,
    ERROR,
    COMPLETE
}