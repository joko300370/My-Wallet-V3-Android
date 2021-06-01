package com.blockchain.nabu.models.responses.simplebuy

import com.blockchain.nabu.datamanagers.OrderInput
import com.blockchain.nabu.datamanagers.OrderOutput
import com.blockchain.nabu.datamanagers.Partner
import com.blockchain.nabu.models.responses.nabu.Address
import com.squareup.moshi.Json
import info.blockchain.balance.CryptoCurrency
import java.util.Date

data class SimpleBuyPairsResp(val pairs: List<SimpleBuyPairResp>)

data class SimpleBuyPairResp(
    val pair: String,
    val buyMin: Long,
    val buyMax: Long,
    val sellMin: Long,
    val sellMax: Long
) {
    fun isCryptoCurrencySupported() =
        CryptoCurrency.values().firstOrNull { it.networkTicker == pair.split("-")[0] } != null
}

data class SimpleBuyEligibility(val simpleBuyTradingEligible: Boolean)

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
    val routingNumber: String?,
    val swiftCode: String?
)

data class SimpleBuyBalanceResponse(
    val pending: String,
    @field:Json(name = "available") // Badly named param, is actually the total including uncleared & locked
    val total: String,
    @field:Json(name = "withdrawable") // Balance that is NOT uncleared and IS withdrawable
    val actionable: String
)

data class AllAssetBalancesResponse(
    @field:Json(name = "BTC")
    val BTC: SimpleBuyBalanceResponse? = null,
    @field:Json(name = "BCH")
    val BCH: SimpleBuyBalanceResponse? = null,
    @field:Json(name = "ETH")
    val ETH: SimpleBuyBalanceResponse? = null,
    @field:Json(name = "XLM")
    val XLM: SimpleBuyBalanceResponse? = null,
    @field:Json(name = "PAX")
    val PAX: SimpleBuyBalanceResponse? = null,
    @field:Json(name = "ALGO")
    val ALGO: SimpleBuyBalanceResponse? = null,
    @field:Json(name = "USDT")
    val USDT: SimpleBuyBalanceResponse? = null,
    @field:Json(name = "WDGLD")
    val WDGLD: SimpleBuyBalanceResponse? = null,
    @field:Json(name = "AAVE")
    val AAVE: SimpleBuyBalanceResponse? = null,
    @field:Json(name = "YFI")
    val YFI: SimpleBuyBalanceResponse? = null,
    @field:Json(name = "DOT")
    val DOT: SimpleBuyBalanceResponse? = null,
    @field:Json(name = "EUR")
    val EUR: SimpleBuyBalanceResponse? = null,
    @field:Json(name = "USD")
    val USD: SimpleBuyBalanceResponse? = null,
    @field:Json(name = "GBP")
    val GBP: SimpleBuyBalanceResponse? = null
) {
    operator fun get(ccy: CryptoCurrency): SimpleBuyBalanceResponse? {
        return when (ccy) {
            CryptoCurrency.BTC -> BTC
            CryptoCurrency.ETHER -> ETH
            CryptoCurrency.BCH -> BCH
            CryptoCurrency.XLM -> XLM
            CryptoCurrency.PAX -> PAX
            CryptoCurrency.ALGO -> ALGO
            CryptoCurrency.USDT -> USDT
            CryptoCurrency.DGLD -> WDGLD
            CryptoCurrency.AAVE -> AAVE
            CryptoCurrency.YFI -> YFI
            CryptoCurrency.DOT -> DOT
            CryptoCurrency.STX -> null
        }
    }

    operator fun get(fiat: String): SimpleBuyBalanceResponse? {
        return when (fiat) {
            "EUR" -> EUR
            "GBP" -> GBP
            "USD" -> USD
            else -> null
        }
    }
}

data class TransferFundsResponse(
    val id: String,
    val code: Long? // Only present in error responses
) {
    companion object {
        const val ERROR_WITHDRAWL_LOCKED = 152L
    }
}

data class FeesResponse(
    val fees: List<CurrencyFeeResponse>,
    val minAmounts: List<CurrencyFeeResponse>
)

data class CurrencyFeeResponse(
    val symbol: String,
    val minorValue: String
)

data class CustodialWalletOrder(
    private val pair: String,
    private val action: String,
    private val input: OrderInput,
    private val output: OrderOutput,
    private val paymentMethodId: String? = null,
    private val paymentType: String? = null
)

data class BuySellOrderResponse(
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
    val attributes: PaymentAttributes?,
    val expiresAt: String,
    val updatedAt: String,
    val side: String,
    val depositPaymentId: String?
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

        const val APPROVAL_ERROR_FAILED = "BANK_TRANSFER_PAYMENT_FAILED"
        const val APPROVAL_ERROR_DECLINED = "BANK_TRANSFER_PAYMENT_DECLINED"
        const val APPROVAL_ERROR_REJECTED = "BANK_TRANSFER_PAYMENT_REJECTED"
        const val APPROVAL_ERROR_EXPIRED = "BANK_TRANSFER_PAYMENT_EXPIRED"
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

class ProductTransferRequestBody(
    val currency: String,
    val amount: String,
    val origin: String,
    val destination: String
)

data class ActivateCardResponse(
    val everypay: EveryPayCardCredentialsResponse?
)

data class EveryPayCardCredentialsResponse(
    val apiUsername: String,
    val mobileToken: String,
    val paymentLink: String
)

data class PaymentAttributes(
    val everypay: EverypayPaymentAttrs?,
    val authorisationUrl: String?,
    val status: String?
)

class PaymentAuthorisationAttrs(
    val authorisationUrl: String
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
    private val paymentMethodId: String?,
    private val attributes: SimpleBuyConfirmationAttributes?,
    private val paymentType: String?
)

data class WithdrawRequestBody(
    private val beneficiary: String,
    private val currency: String,
    private val amount: String
)

data class DepositRequestBody(
    private val currency: String,
    private val depositAddress: String,
    private val txHash: String,
    private val amount: String,
    private val product: String
)

data class WithdrawLocksCheckRequestBody(
    private val paymentMethod: String,
    private val product: String,
    private val currency: String
)

data class WithdrawLocksCheckResponse(
    val rule: WithdrawLocksRuleResponse?
)

data class WithdrawLocksRuleResponse(
    val lockTime: String
)

data class TransactionsResponse(
    val items: List<TransactionResponse>
)

data class TransactionResponse(
    val id: String,
    val amount: AmountResponse,
    val amountMinor: String,
    val feeMinor: String?,
    val insertedAt: String,
    val type: String,
    val state: String,
    val extraAttributes: TransactionAttributesResponse,
    val txHash: String?
) {
    companion object {
        const val COMPLETE = "COMPLETE"
        const val CREATED = "CREATED"
        const val PENDING = "PENDING"
        const val UNIDENTIFIED = "UNIDENTIFIED"
        const val FAILED = "FAILED"
        const val FRAUD_REVIEW = "FRAUD_REVIEW"
        const val CLEARED = "CLEARED"
        const val REJECTED = "REJECTED"
        const val MANUAL_REVIEW = "MANUAL_REVIEW"
        const val REFUNDED = "REFUNDED"

        const val DEPOSIT = "DEPOSIT"
        const val CHARGE = "CHARGE"
        const val WITHDRAWAL = "WITHDRAWAL"
    }
}

data class TransactionAttributesResponse(
    val beneficiary: TransactionBeneficiaryResponse
)

data class TransactionBeneficiaryResponse(
    val accountRef: String
)

data class AmountResponse(
    val symbol: String
)

data class SimpleBuyConfirmationAttributes(
    private val everypay: EveryPayAttrs? = null,
    private val callback: String? = null
)

data class EveryPayAttrs(private val customerUrl: String)

typealias BuyOrderListResponse = List<BuySellOrderResponse>