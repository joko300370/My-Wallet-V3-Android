package com.blockchain.nabu.models.responses.simplebuy

data class RecurringBuyRequestBody(
    private val inputValue: String,
    private val inputCurrency: String,
    private val destinationCurrency: String,
    private val paymentMethod: String,
    private val period: String,
    private val nextPayment: String? = null,
    private val beneficiaryId: String? = null
)