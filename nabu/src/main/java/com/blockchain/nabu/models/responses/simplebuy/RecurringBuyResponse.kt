package com.blockchain.nabu.models.responses.simplebuy

data class RecurringBuyResponse(
    val id: String,
    val userId: String,
    val inputValue: String,
    val inputCurrency: String,
    val period: String,
    val destinationCurrency: String,
    val paymentMethod: String,
    val state: String
) {
    companion object {
        const val ACTIVE = "ACTIVE"
        const val NOT_ACTIVE = "NOT_ACTIVE"
    }
}