package com.blockchain.swap.nabu.datamanagers

import com.blockchain.swap.nabu.datamanagers.custodialwalletimpl.CardStatus
import com.blockchain.swap.nabu.models.simplebuy.CardPartnerAttributes
import com.blockchain.swap.nabu.models.simplebuy.CardPaymentAttributes
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import com.braintreepayments.cardform.utils.CardType
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import java.io.Serializable
import java.util.Date

enum class OrderState {
    UNKNOWN,
    UNINITIALISED,
    INITIALISED,
    PENDING_CONFIRMATION, // Has created but not confirmed
    AWAITING_FUNDS, // Waiting for a bank transfer etc
    PENDING_EXECUTION, // Funds received, but crypto not yet released (don't know if we'll need this?)
    FINISHED,
    CANCELED,
    FAILED
}

// inject an instance of this to provide simple buy and custodial balance/transfer services.
// In the short-term, use aa instance which provides mock data - for development and testing.
// Once the UI and business logic are all working, we can then have NabuDataManager - or something similar -
// implement this, and use koin.bind to have that instance injected instead to provide live data

interface CustodialWalletManager {

    fun getBalanceForAsset(
        crypto: CryptoCurrency
    ): Maybe<CryptoValue>

    fun getBuyLimitsAndSupportedCryptoCurrencies(
        nabuOfflineTokenResponse: NabuOfflineTokenResponse,
        fiatCurrency: String
    ): Single<SimpleBuyPairs>

    fun getSupportedFiatCurrencies(
        nabuOfflineTokenResponse: NabuOfflineTokenResponse
    ): Single<List<String>>

    fun getQuote(action: String, crypto: CryptoCurrency, amount: FiatValue): Single<Quote>

    fun createOrder(
        cryptoCurrency: CryptoCurrency,
        amount: FiatValue,
        action: String,
        paymentMethodId: String? = null,
        stateAction: String? = null
    ): Single<BuyOrder>

    fun getPredefinedAmounts(
        currency: String
    ): Single<List<FiatValue>>

    fun getBankAccountDetails(
        currency: String
    ): Single<BankAccount>

    fun isEligibleForSimpleBuy(fiatCurrency: String): Single<Boolean>

    fun isCurrencySupportedForSimpleBuy(
        fiatCurrency: String
    ): Single<Boolean>

    fun getOutstandingBuyOrders(crypto: CryptoCurrency): Single<BuyOrderList>
    fun getAllOutstandingBuyOrders(): Single<BuyOrderList>
    fun getAllBuyOrdersFor(crypto: CryptoCurrency): Single<BuyOrderList>

    fun getBuyOrder(orderId: String): Single<BuyOrder>

    fun deleteBuyOrder(orderId: String): Completable

    fun deleteCard(cardId: String): Completable

    fun transferFundsToWallet(amount: CryptoValue, walletAddress: String): Completable

    // For test/dev
    fun cancelAllPendingBuys(): Completable

    fun updateSupportedCardTypes(fiatCurrency: String, isTier2Approved: Boolean): Completable

    fun fetchSuggestedPaymentMethod(
        fiatCurrency: String,
        isTier2Approved: Boolean
    ): Single<List<PaymentMethod>>

    fun addNewCard(fiatCurrency: String, billingAddress: BillingAddress): Single<CardToBeActivated>

    fun activateCard(cardId: String, attributes: CardPartnerAttributes): Single<PartnerCredentials>

    fun getCardDetails(cardId: String): Single<PaymentMethod.Card>

    fun fetchUnawareLimitsCards(
        states: List<CardStatus>
    ): Single<List<PaymentMethod.Card>> // fetches the available

    fun confirmOrder(orderId: String, attributes: CardPartnerAttributes?): Single<BuyOrder>

    fun getInterestAccountDetails(crypto: CryptoCurrency): Maybe<CryptoValue>

    fun getInterestAccountRates(crypto: CryptoCurrency): Single<Double>
}

data class BuyOrder(
    val id: String,
    val pair: String,
    val fiat: FiatValue,
    val crypto: CryptoValue,
    val paymentMethodId: String,
    val state: OrderState = OrderState.UNINITIALISED,
    val expires: Date = Date(),
    val updated: Date = Date(),
    val created: Date = Date(),
    val fee: FiatValue? = null,
    val price: FiatValue? = null,
    val orderValue: CryptoValue? = null,
    val attributes: CardPaymentAttributes? = null
)

typealias BuyOrderList = List<BuyOrder>

data class OrderInput(private val symbol: String, private val amount: String)

data class OrderOutput(private val symbol: String)

data class SimpleBuyPairs(val pairs: List<SimpleBuyPair>)

data class SimpleBuyPair(private val pair: String, val buyLimits: BuyLimits) {
    val cryptoCurrency: CryptoCurrency
        get() = CryptoCurrency.values().first { it.networkTicker == pair.split("-")[0] }
    val fiatCurrency: String = pair.split("-")[1]
}

data class BuyLimits(private val min: Long, private val max: Long) {
    fun minLimit(currency: String): FiatValue = FiatValue.fromMinor(currency, min)
    fun maxLimit(currency: String): FiatValue = FiatValue.fromMinor(currency, max)
}

data class Quote(
    val date: Date,
    val fee: FiatValue,
    val estimatedAmount: CryptoValue,
    val rate: FiatValue
)

data class BankAccount(val details: List<BankDetail>)

data class BankDetail(val title: String, val value: String, val isCopyable: Boolean = false)

sealed class SimpleBuyError : Throwable() {
    object OrderLimitReached : SimpleBuyError()
    object OrderNotCancelable : SimpleBuyError()
    object WithdrawlAlreadyPending : SimpleBuyError()
    object WithdrawlInsufficientFunds : SimpleBuyError()
}

sealed class PaymentMethod(val id: String, open val limits: PaymentLimits?) : Serializable {
    object Undefined : PaymentMethod(UNDEFINED_PAYMENT_ID, null)
    data class BankTransfer(override val limits: PaymentLimits) :
        PaymentMethod(BANK_PAYMENT_ID, limits)

    data class UndefinedCard(override val limits: PaymentLimits) :
        PaymentMethod(UNDEFINED_CARD_PAYMENT_ID, limits)

    data class Card(
        val cardId: String,
        override val limits: PaymentLimits,
        private val label: String,
        val endDigits: String,
        val partner: Partner,
        val expireDate: Date,
        val cardType: CardType,
        val status: CardStatus
    ) : PaymentMethod(cardId, limits), Serializable {
        fun uiLabelWithDigits() =
            "${uiLabel()} ${dottedEndDigits()}"

        fun uiLabel() =
            label.takeIf { it.isNotEmpty() } ?: cardType.label()

        fun dottedEndDigits() =
            "•••• $endDigits"

        private fun CardType.label(): String =
            when (this) {
                CardType.VISA -> "Visa"
                CardType.MASTERCARD -> "Mastercard"
                CardType.AMEX -> "American Express"
                CardType.DINERS_CLUB -> "Diners Club"
                CardType.MAESTRO -> "Maestro"
                CardType.JCB -> "JCB"
                else -> ""
            }
    }

    companion object {
        const val BANK_PAYMENT_ID = "BANK_PAYMENT_ID"
        const val UNDEFINED_PAYMENT_ID = "UNDEFINED_PAYMENT_ID"
        const val UNDEFINED_CARD_PAYMENT_ID = "UNDEFINED_CARD_PAYMENT_ID"
    }
}

data class PaymentLimits(val min: FiatValue, val max: FiatValue) : Serializable {
    constructor(min: Long, max: Long, currency: String) : this(
        FiatValue.fromMinor(currency, min),
        FiatValue.fromMinor(currency, max)
    )
}

data class BillingAddress(
    val countryCode: String,
    val fullName: String,
    val addressLine1: String,
    val addressLine2: String,
    val city: String,
    val postCode: String,
    val state: String?
)

data class CardToBeActivated(val partner: Partner, val cardId: String)

data class PartnerCredentials(val everypay: EveryPayCredentials?)

data class EveryPayCredentials(
    val apiUsername: String,
    val mobileToken: String,
    val paymentLink: String
)

enum class Partner {
    EVERYPAY,
    UNKNOWN
}