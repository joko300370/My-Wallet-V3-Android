package com.blockchain.nabu.models.responses.simplebuy

import com.blockchain.nabu.datamanagers.RecurringBuyActivityState
import com.blockchain.nabu.datamanagers.RecurringBuyOrder
import com.blockchain.nabu.datamanagers.RecurringBuyTransaction
import com.blockchain.nabu.datamanagers.custodialwalletimpl.toPaymentMethodType
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.nabu.models.data.RecurringBuyFrequency
import com.blockchain.nabu.models.data.RecurringBuyState
import com.blockchain.utils.fromIso8601ToUtc
import com.blockchain.utils.toLocalTime
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import java.util.Date
import java.util.UnknownFormatConversionException

data class RecurringBuyEligibilityResponse(
    val eligibleMethods: List<String>
)

data class RecurringBuyResponse(
    val id: String,
    val userId: String,
    val inputCurrency: String,
    val inputValue: String,
    val destinationCurrency: String,
    val paymentMethod: String,
    val period: String,
    val nextPayment: String,
    val state: String,
    val insertedAt: String,
    val updatedAt: String
) {
    companion object {
        const val ACTIVE = "ACTIVE"
        const val NOT_ACTIVE = "NOT_ACTIVE"
        const val DAILY = "DAILY"
        const val WEEKLY = "WEEKLY"
        const val BI_WEEKLY = "BI_WEEKLY"
        const val MONTHLY = "MONTHLY"
    }
}

fun RecurringBuyResponse.toRecurringBuy(): RecurringBuy? {
    val crypto = CryptoCurrency.fromNetworkTicker(destinationCurrency)
    return crypto?.let {
        RecurringBuy(
            id = id,
            state = recurringBuyState(),
            recurringBuyFrequency = period.toRecurringBuyFrequency(),
            nextPaymentDate = nextPayment.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
            paymentMethodType = paymentMethod.toPaymentMethodType(),
            amount = FiatValue.fromMinor(inputCurrency, inputValue.toLong()),
            asset = it
        )
    }
}

private fun RecurringBuyResponse.recurringBuyState() =
    when (state) {
        RecurringBuyResponse.ACTIVE -> RecurringBuyState.ACTIVE
        RecurringBuyResponse.NOT_ACTIVE -> RecurringBuyState.NOT_ACTIVE
        else -> throw IllegalStateException("Unsupported recurring state")
    }

fun RecurringBuyResponse.toRecurringBuyOrder(): RecurringBuyOrder =
    RecurringBuyOrder(state = recurringBuyState())

private fun String.toRecurringBuyFrequency(): RecurringBuyFrequency =
    when (this) {
        RecurringBuyResponse.DAILY -> RecurringBuyFrequency.DAILY
        RecurringBuyResponse.WEEKLY -> RecurringBuyFrequency.WEEKLY
        RecurringBuyResponse.BI_WEEKLY -> RecurringBuyFrequency.BI_WEEKLY
        RecurringBuyResponse.MONTHLY -> RecurringBuyFrequency.MONTHLY
        else -> RecurringBuyFrequency.UNKNOWN
    }

data class RecurringBuyTransactionResponse(
    val id: String,
    val recurringBuyId: String,
    val state: String,
    val originValue: String,
    val originCurrency: String,
    val destinationValue: String,
    val destinationCurrency: String,
    val insertedAt: String
) {
    companion object {
        const val COMPLETED = "COMPLETE"
        const val CREATED = "CREATED"
        const val PENDING = "PENDING"
        const val FAILED = "FAILED"
    }
}

fun RecurringBuyTransactionResponse.toRecurringBuyTransaction(): RecurringBuyTransaction {
    val cryptoCurrency =
        CryptoCurrency.fromNetworkTicker(destinationCurrency)
            ?: throw UnknownFormatConversionException("Unknown Crypto currency: $destinationCurrency")

    return RecurringBuyTransaction(
        id = id,
        recurringBuyId = recurringBuyId,
        state = state.toRecurringBuyActivityState(),
        destinationValue = CryptoValue.fromMinor(cryptoCurrency, destinationValue.toBigInteger()),
        originMoney = FiatValue.fromMinor(originCurrency, originValue.toLong()),
        insertedAt = insertedAt.fromIso8601ToUtc()?.toLocalTime() ?: Date()
    )
}

fun String.toRecurringBuyActivityState() =
    when (this) {
        RecurringBuyTransactionResponse.COMPLETED -> RecurringBuyActivityState.COMPLETED
        RecurringBuyTransactionResponse.PENDING,
        RecurringBuyTransactionResponse.CREATED -> RecurringBuyActivityState.PENDING
        RecurringBuyTransactionResponse.FAILED -> RecurringBuyActivityState.FAILED
        else -> RecurringBuyActivityState.UNKNOWN
    }