package com.blockchain.nabu.models.responses.banktransfer

data class CreateLinkBankResponse(
    val partner: String,
    val id: String,
    val attributes: LinkBankAttrsResponse?
) {
    companion object {
        const val YODLEE_PARTNER = "YODLEE"
        const val YAPILY_PARTNER = "YAPILY"
    }
}

data class CreateLinkBankRequestBody(
    private val currency: String
)

data class LinkBankAttrsResponse(
    val token: String?,
    val fastlinkUrl: String?,
    val fastlinkParams: FastlinkParamsResponse?,
    val institutions: List<YapilyInstitutionResponse>?,
    val entity: String?
)

data class YapilyInstitutionResponse(
    val countries: List<YapilyCountryResponse>,
    val fullName: String,
    val id: String,
    val media: List<YapilyMediaResponse>
)

data class YapilyCountryResponse(val countryCode2: String, val displayName: String)

data class YapilyMediaResponse(val source: String, val type: String)

data class FastlinkParamsResponse(
    val configName: String
)

data class LinkedBankTransferResponse(
    val id: String,
    val partner: String,
    val currency: String,
    val state: String,
    val details: LinkedBankDetailsResponse?,
    val error: String?,
    val attributes: LinkedBankTransferAttributesResponse?
) {
    companion object {
        const val CREATED = "CREATED"
        const val ACTIVE = "ACTIVE"
        const val PENDING = "PENDING"
        const val BLOCKED = "BLOCKED"
        const val FRAUD_REVIEW = "FRAUD_REVIEW"
        const val MANUAL_REVIEW = "MANUAL_REVIEW"

        const val ERROR_ALREADY_LINKED = "BANK_TRANSFER_ACCOUNT_ALREADY_LINKED"
        const val ERROR_UNSUPPORTED_ACCOUNT = "BANK_TRANSFER_ACCOUNT_INFO_NOT_FOUND"
        const val ERROR_NAMES_MISMATCHED = "BANK_TRANSFER_ACCOUNT_NAME_MISMATCH"
        const val ERROR_ACCOUNT_EXPIRED = "BANK_TRANSFER_ACCOUNT_EXPIRED"
        const val ERROR_ACCOUNT_REJECTED = "BANK_TRANSFER_ACCOUNT_REJECTED"
        const val ERROR_ACCOUNT_FAILURE = "BANK_TRANSFER_ACCOUNT_FAILED"
    }
}

data class LinkedBankTransferAttributesResponse(
    val authorisationUrl: String?,
    val entity: String?,
    val media: List<BankMediaResponse>?
)

data class ProviderAccountAttrs(
    val providerAccountId: String? = null,
    val accountId: String? = null,
    val institutionId: String? = null,
    val callback: String? = null
)

data class UpdateProviderAccountBody(
    val attributes: ProviderAccountAttrs
)

data class LinkedBankDetailsResponse(
    val accountNumber: String,
    val accountName: String,
    val bankName: String,
    val bankAccountType: String,
    val sortCode: String?,
    val iban: String?,
    val bic: String?
)

data class BankTransferPaymentBody(
    val amountMinor: String,
    val currency: String,
    val product: String = "SIMPLEBUY"
)

data class BankTransferPaymentResponse(
    val paymentId: String,
    val bankAccountType: String?
)

data class BankInfoResponse(
    val id: String,
    val name: String,
    val accountName: String?,
    val currency: String,
    val state: String,
    val accountNumber: String?,
    val bankAccountType: String?,
    val isBankAccount: Boolean,
    val isBankTransferAccount: Boolean,
    val attributes: BankInfoAttributes?
) {
    companion object {
        const val ACTIVE = "ACTIVE"
        const val PENDING = "PENDING"
        const val BLOCKED = "BLOCKED"
    }
}

data class BankInfoAttributes(
    val entity: String,
    val media: List<BankMediaResponse>?,
    val status: String,
    val authorisationUrl: String
)

data class BankMediaResponse(
    val source: String,
    val type: String
) {
    companion object {
        const val ICON = "icon"
        const val LOGO = "logo"
    }
}

object WithdrawFeeRequest {
    const val BANK_TRANSFER = "BANK_TRANSFER"
    const val BANK_ACCOUNT = "BANK_ACCOUNT"
}
