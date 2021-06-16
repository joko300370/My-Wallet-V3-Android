package com.blockchain.nabu.models.responses.simplebuy

import com.blockchain.nabu.datamanagers.RecurringBuyErrorState
import com.blockchain.nabu.datamanagers.RecurringBuyOrder
import com.blockchain.nabu.datamanagers.RecurringBuyTransaction
import com.blockchain.nabu.datamanagers.RecurringBuyTransactionState
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
    val paymentMethodId: String?,
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
            asset = it,
            createDate = insertedAt.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
            paymentMethodId = paymentMethodId
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
    val failureReason: String?,
    val originValue: String,
    val originCurrency: String,
    val destinationValue: String?,
    val destinationCurrency: String,
    val paymentMethod: String,
    val paymentMethodId: String?,
    val nextPayment: String,
    val period: String,
    val fee: String?,
    val insertedAt: String
) {
    companion object {
        const val COMPLETED = "COMPLETE"
        const val CREATED = "CREATED"
        const val PENDING = "PENDING"
        const val FAILED = "FAILED"
        const val FAILED_INSUFFICIENT_FUNDS = "Insufficient funds"
        const val FAILED_INTERNAL_ERROR = "Internal server error"
        const val FAILED_BENEFICIARY_BLOCKED = "Beneficiary missed/blocked"
        const val FAILED_LIMITS_EXCEED = "User trading limits exceeded"
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
        failureReason = failureReason?.toRecurringBuyError(),
        destinationMoney = destinationValue?.let {
            CryptoValue.fromMinor(
                cryptoCurrency, destinationValue.toBigInteger()
            )
        } ?: CryptoValue.zero(cryptoCurrency),
        originMoney = FiatValue.fromMinor(originCurrency, originValue.toLong()),
        paymentMethodId = paymentMethodId,
        paymentMethod = paymentMethod.toPaymentMethodType(),
        fee = fee?.let { FiatValue.fromMinor(originCurrency, fee.toLong()) } ?: FiatValue.zero(originCurrency),
        period = period.toRecurringBuyFrequency(),
        nextPayment = nextPayment.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
        insertedAt = insertedAt.fromIso8601ToUtc()?.toLocalTime() ?: Date()
    )
}

fun String.toRecurringBuyActivityState() =
    when (this) {
        RecurringBuyTransactionResponse.COMPLETED -> RecurringBuyTransactionState.COMPLETED
        RecurringBuyTransactionResponse.PENDING,
        RecurringBuyTransactionResponse.CREATED -> RecurringBuyTransactionState.PENDING
        RecurringBuyTransactionResponse.FAILED -> RecurringBuyTransactionState.FAILED
        else -> RecurringBuyTransactionState.UNKNOWN
    }

fun String.toRecurringBuyError() =
    when (this) {
        RecurringBuyTransactionResponse.FAILED_INSUFFICIENT_FUNDS ->
            RecurringBuyErrorState.INSUFFICIENT_FUNDS
        RecurringBuyTransactionResponse.FAILED_INTERNAL_ERROR ->
            RecurringBuyErrorState.INTERNAL_SERVER_ERROR
        RecurringBuyTransactionResponse.FAILED_BENEFICIARY_BLOCKED ->
            RecurringBuyErrorState.BLOCKED_BENEFICIARY_ID
        RecurringBuyTransactionResponse.FAILED_LIMITS_EXCEED ->
            RecurringBuyErrorState.TRADING_LIMITS_EXCEED
        else -> RecurringBuyErrorState.UNKNOWN
    }