package com.blockchain.swap.nabu.datamanagers.custodialwalletimpl

import com.blockchain.swap.nabu.datamanagers.BankAccount
import com.blockchain.swap.nabu.datamanagers.BuyOrder
import com.blockchain.swap.nabu.datamanagers.BuyOrderList
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.Quote
import com.blockchain.swap.nabu.datamanagers.SimpleBuyPairs
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import om.blockchain.swap.nabu.BuildConfig

class CustodialWalletManagerSwitcher(
    private val mockCustodialWalletManager: StubCustodialWalletManager,
    private val liveCustodialWalletManager: LiveCustodialWalletManager
) : CustodialWalletManager {

    @Suppress("ConstantConditionIf")
    private val proxy: CustodialWalletManager by lazy {
        if (BuildConfig.USE_MOCK_SIMPLE_BUY_BACKEND)
            mockCustodialWalletManager
        else
            liveCustodialWalletManager
    }

    override fun getBuyLimitsAndSupportedCryptoCurrencies(
        nabuOfflineTokenResponse: NabuOfflineTokenResponse,
        currency: String
    ): Single<SimpleBuyPairs> =
        proxy.getBuyLimitsAndSupportedCryptoCurrencies(nabuOfflineTokenResponse, currency)

    override fun getQuote(action: String, crypto: CryptoCurrency, amount: FiatValue): Single<Quote> =
        proxy.getQuote(action, crypto, amount)

    override fun createOrder(
        cryptoCurrency: CryptoCurrency,
        amount: FiatValue,
        action: String
    ): Single<BuyOrder> =
        proxy.createOrder(
            cryptoCurrency,
            amount,
            action
        )

    override fun getPredefinedAmounts(currency: String): Single<List<FiatValue>> =
        proxy.getPredefinedAmounts(currency)

    override fun getBankAccountDetails(currency: String): Single<BankAccount> =
        proxy.getBankAccountDetails(currency)

    override fun isEligibleForSimpleBuy(): Single<Boolean> =
        proxy.isEligibleForSimpleBuy()

    override fun getBalanceForAsset(crypto: CryptoCurrency): Maybe<CryptoValue> =
        proxy.getBalanceForAsset(crypto)

    override fun isCurrencySupportedForSimpleBuy(currency: String): Single<Boolean> =
        proxy.isCurrencySupportedForSimpleBuy(currency)

    override fun getOutstandingBuyOrders(): Single<BuyOrderList> =
        proxy.getOutstandingBuyOrders()

    override fun getBuyOrder(orderId: String): Single<BuyOrder> =
        proxy.getBuyOrder(orderId)

    override fun deleteBuyOrder(orderId: String): Completable =
        proxy.deleteBuyOrder(orderId)

    override fun transferFundsToWallet(amount: CryptoValue, walletAddress: String): Completable =
        proxy.transferFundsToWallet(amount, walletAddress)

    override fun cancelAllPendingBuys(): Completable =
        proxy.cancelAllPendingBuys()
}
