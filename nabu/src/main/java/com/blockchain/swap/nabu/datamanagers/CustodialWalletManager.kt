package com.blockchain.swap.nabu.datamanagers

import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import java.util.Date

enum class OrderState {
    UNINITIALISED,
    INITIALISED,
    AWAITING_FUNDS, // Waiting for a bank transfer etc
    PENDING, // Funds received, but crypto not yet released (don't know if we'll need this?)
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
        currency: String
    ): Single<SimpleBuyPairs>

    fun getQuote(action: String, crypto: CryptoCurrency, amount: FiatValue): Single<Quote>

    fun createOrder(
        cryptoCurrency: CryptoCurrency,
        amount: FiatValue,
        action: String
    ): Single<BuyOrder>

    fun getPredefinedAmounts(
        currency: String
    ): Single<List<FiatValue>>

    fun getBankAccountDetails(
        currency: String
    ): Single<BankAccount>

    fun isEligibleForSimpleBuy(): Single<Boolean>

    fun isCurrencySupportedForSimpleBuy(
        currency: String
    ): Single<Boolean>

    fun getOutstandingBuyOrders(): Single<BuyOrderList>

    fun getBuyOrder(orderId: String): Single<BuyOrder>

    fun deleteBuyOrder(orderId: String): Completable

    fun transferFundsToWallet(amount: CryptoValue, walletAddress: String): Completable

    // For test/dev
    fun cancelAllPendingBuys(): Completable
}

data class BuyOrder(
    val id: String,
    val pair: String,
    val fiat: FiatValue,
    val crypto: CryptoValue,
    val state: OrderState = OrderState.UNINITIALISED,
    val expires: Date = Date()
)

typealias BuyOrderList = List<BuyOrder>

data class OrderInput(private val symbol: String, private val amount: String)

data class OrderOutput(private val symbol: String)

data class SimpleBuyPairs(val pairs: List<SimpleBuyPair>)

data class SimpleBuyPair(private val pair: String, val buyLimits: BuyLimits) {
    val cryptoCurrency: CryptoCurrency
        get() = CryptoCurrency.values().first { it.symbol == pair.split("-")[0] }
    val fiatCurrency: String = pair.split("-")[1]
}

data class BuyLimits(private val min: Long, private val max: Long) {
    fun minLimit(currency: String): FiatValue = FiatValue.fromMinor(currency, min)
    fun maxLimit(currency: String): FiatValue = FiatValue.fromMinor(currency, max)
}

data class Quote(val date: Date)

data class BankAccount(val details: List<BankDetail>)

data class BankDetail(val title: String, val value: String, val isCopyable: Boolean = false)

sealed class SimpleBuyError : Throwable() {
    object OrderLimitReached : SimpleBuyError()
    object OrderNotCancelable : SimpleBuyError()
    object WithdrawlAlreadyPending : SimpleBuyError()
    object WithdrawlInsufficientFunds : SimpleBuyError()
}