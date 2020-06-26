package com.blockchain.swap.nabu.models.cards

import com.braintreepayments.cardform.utils.CardType

data class PaymentMethodsResponse(
    val currency: String,
    val methods: List<PaymentMethodResponse>
)

data class BeneficiariesResponse(
    val id: String,
    val address: String,
    val currency: String,
    val name: String,
    val agent: AgentResponse
)

data class AgentResponse(val account: String)

data class PaymentMethodResponse(
    val type: String,
    val limits: Limits,
    val subTypes: List<String>?,
    val currency: String?
) {
    companion object {
        const val BANK_ACCOUNT = "BANK_ACCOUNT"
        const val PAYMENT_CARD = "PAYMENT_CARD"
        const val FUNDS = "FUNDS"
    }
}

data class Limits(val min: Long, val max: Long)

data class CardResponse(
    val id: String,
    val partner: String,
    val state: String,
    val currency: String,
    val card: CardDetailsResponse?
) {
    companion object {
        const val ACTIVE = "ACTIVE"
        const val PENDING = "PENDING"
        const val BLOCKED = "BLOCKED"
        const val CREATED = "CREATED"
        const val EXPIRED = "EXPIRED"
    }
}

data class CardDetailsResponse(
    val number: String,
    val expireYear: Int,
    val expireMonth: Int,
    val type: CardType,
    val label: String
)