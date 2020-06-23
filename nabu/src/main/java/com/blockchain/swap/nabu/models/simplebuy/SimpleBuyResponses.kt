package com.blockchain.swap.nabu.models.simplebuy

import com.blockchain.swap.nabu.datamanagers.OrderInput
import com.blockchain.swap.nabu.datamanagers.OrderOutput
import com.blockchain.swap.nabu.datamanagers.Partner
import com.blockchain.swap.nabu.models.nabu.Address
import com.squareup.moshi.Json
import info.blockchain.balance.CryptoCurrency

import java.util.Date

data class SimpleBuyPairsResp(val pairs: List<SimpleBuyPairResp>)

data class SimpleBuyPairResp(val pair: String, val buyMin: Long, val buyMax: Long) {
    fun isCryptoCurrencySupported() =
        CryptoCurrency.values().firstOrNull { it.networkTicker == pair.split("-")[0] } != null
}

data class SimpleBuyEligibility(val eligible: Boolean)

data class SimpleBuyCurrency(val currency: String)

data class SimpleBuyQuoteResponse(
    val time: Date,
    val rate: Long,
    val rateWithoutFee: Long,
/* the  fee value is more of a feeRate (ie it is the fee per 1 unit of crypto) to get the actual
 "fee" you'll need to multiply by amount of crypto
 */
    val fee: Long
)

data class BankAccountResponse(val address: String?, val agent: BankAgentResponse, val currency: String)

data class BankAgentResponse(
    val account: String?,
    val address: String?,
    val code: String?,
    val country: String?,
    val name: String?,
    val recipient: String?,
    val routingNumber: String?
)

data class SimpleBuyBalanceResponse(
    val available: String
)

data class SimpleBuyAllBalancesResponse(
    @Json(name = "BTC")
    val BTC: SimpleBuyBalanceResponse? = null,
    @Json(name = "BCH")
    val BCH: SimpleBuyBalanceResponse? = null,
    @Json(name = "ETH")
    val ETH: SimpleBuyBalanceResponse? = null,
    @Json(name = "XLM")
    val XLM: SimpleBuyBalanceResponse? = null,
    @Json(name = "PAX")
    val PAX: SimpleBuyBalanceResponse? = null,
    @Json(name = "ALGO")
    val ALGO: SimpleBuyBalanceResponse? = null
) {
    operator fun get(ccy: CryptoCurrency): String? {
        return when (ccy) {
            CryptoCurrency.BTC -> BTC
            CryptoCurrency.ETHER -> ETH
            CryptoCurrency.BCH -> BCH
            CryptoCurrency.XLM -> XLM
            CryptoCurrency.PAX -> PAX
            CryptoCurrency.ALGO -> ALGO
            else -> null
        }?.available
    }
}

data class CustodialWalletOrder(
    private val pair: String,
    private val action: String,
    private val input: OrderInput,
    private val output: OrderOutput,
    private val paymentMethodId: String?,
    private val paymentType: String
)

data class BuyOrderResponse(
    val id: String,
    val pair: String,
    val inputCurrency: String,
    val inputQuantity: String,
    val outputCurrency: String,
    val outputQuantity: String,
    val paymentMethodId: String?,
    val paymentType: String,
    val state: String,
    val insertedAt: String,
    val price: String?,
    val fee: String?,
    val attributes: CardPaymentAttributes?,
    val expiresAt: String,
    val updatedAt: String
) {
    companion object {
        const val PENDING_DEPOSIT = "PENDING_DEPOSIT"
        const val PENDING_EXECUTION = "PENDING_EXECUTION"
        const val PENDING_CONFIRMATION = "PENDING_CONFIRMATION"
        const val DEPOSIT_MATCHED = "DEPOSIT_MATCHED"
        const val FINISHED = "FINISHED"
        const val CANCELED = "CANCELED"
        const val FAILED = "FAILED"
        const val EXPIRED = "EXPIRED"
    }
}

data class TransferRequest(
    val address: String,
    val currency: String,
    val amount: String
)

data class AddNewCardBodyRequest(private val currency: String, private val address: Address)

data class AddNewCardResponse(
    val id: String,
    val partner: Partner
)

data class ActivateCardResponse(
    val everypay: EveryPayCardCredentialsResponse?
)

data class EveryPayCardCredentialsResponse(
    val apiUsername: String,
    val mobileToken: String,
    val paymentLink: String
)

data class CardPaymentAttributes(
    val everypay: EverypayPaymentAttrs?
)

data class EverypayPaymentAttrs(
    val paymentLink: String,
    val paymentState: String
) {
    companion object {
        const val WAITING_3DS = "WAITING_FOR_3DS_RESPONSE"
    }
}

data class ConfirmOrderRequestBody(
    private val action: String = "confirm",
    private val attributes: CardPartnerAttributes?
)

data class CardPartnerAttributes(
    private val everypay: EveryPayAttrs?
)

data class EveryPayAttrs(private val customerUrl: String)

typealias BuyOrderListResponse = List<BuyOrderResponse>