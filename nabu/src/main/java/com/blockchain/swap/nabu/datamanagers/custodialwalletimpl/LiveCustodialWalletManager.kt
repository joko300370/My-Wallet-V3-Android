package com.blockchain.swap.nabu.datamanagers.custodialwalletimpl

import com.blockchain.swap.nabu.Authenticator
import com.blockchain.swap.nabu.datamanagers.BankAccount
import com.blockchain.swap.nabu.datamanagers.BuyLimits
import com.blockchain.swap.nabu.datamanagers.BuyOrder
import com.blockchain.swap.nabu.datamanagers.BuyOrderList
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.OrderInput
import com.blockchain.swap.nabu.datamanagers.OrderOutput
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.swap.nabu.datamanagers.Quote
import com.blockchain.swap.nabu.datamanagers.SimpleBuyPair
import com.blockchain.swap.nabu.datamanagers.SimpleBuyPairs
import com.blockchain.swap.nabu.extensions.toLocalTime
import com.blockchain.swap.nabu.models.simplebuy.BuyOrderResponse
import com.blockchain.swap.nabu.models.simplebuy.BankAccountResponse
import com.blockchain.swap.nabu.models.simplebuy.CustodialWalletOrder
import com.blockchain.swap.nabu.models.simplebuy.OrderStateResponse
import com.blockchain.swap.nabu.models.simplebuy.TransferRequest
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.swap.nabu.service.NabuService
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.flatMapIterable
import okhttp3.internal.toLongOrDefault
import java.math.BigDecimal
import java.util.UnknownFormatConversionException

class LiveCustodialWalletManager(
    private val nabuService: NabuService,
    private val authenticator: Authenticator,
    private val paymentAccountMapperMappers: Map<String, PaymentAccountMapper>
) : CustodialWalletManager {

    override fun getQuote(action: String, crypto: CryptoCurrency, amount: FiatValue): Single<Quote> =
        authenticator.authenticate {
            nabuService.getSimpleBuyQuote(
                sessionToken = it,
                action = action,
                currencyPair = "${crypto.symbol}-${amount.currencyCode}",
                amount = amount.valueMinor.toString()
            )
        }.map { quoteResponse ->
            Quote(date = quoteResponse.time.toLocalTime())
        }

    override fun createOrder(
        cryptoCurrency: CryptoCurrency,
        amount: FiatValue,
        action: String
    ): Single<BuyOrder> =
        authenticator.authenticate {
            nabuService.createOrder(
                it,
                CustodialWalletOrder(
                    pair = "${cryptoCurrency.symbol}-${amount.currencyCode}",
                    action = action,
                    input = OrderInput(
                        amount.currencyCode, amount.valueMinor.toString()
                    ),
                    output = OrderOutput(
                        cryptoCurrency.symbol
                    )
                )
            )
        }.map { response -> response.toBuyOrder() }

    override fun getBuyLimitsAndSupportedCryptoCurrencies(
        nabuOfflineTokenResponse: NabuOfflineTokenResponse,
        fiatCurrency: String
    ): Single<SimpleBuyPairs> =
        authenticator.authenticate {
            nabuService.getSupportedCurrencies(fiatCurrency)
        }.map {
            SimpleBuyPairs(it.pairs.map { responsePair ->
                SimpleBuyPair(
                    responsePair.pair,
                    BuyLimits(
                        responsePair.buyMin,
                        responsePair.buyMax
                    )
                )
            })
        }

    override fun getPredefinedAmounts(currency: String): Single<List<FiatValue>> =
        authenticator.authenticate {
            nabuService.getPredefinedAmounts(it, currency)
        }.map { response ->
            val currencyAmounts = response.firstOrNull { it[currency] != null } ?: emptyMap()
            currencyAmounts[currency]?.map { value ->
                FiatValue.fromMinor(currency, value)
            } ?: emptyList()
        }

    override fun getBankAccountDetails(currency: String): Single<BankAccount> =
        authenticator.authenticate {
            nabuService.getSimpleBuyBankAccountDetails(it, currency)
        }.map { response ->
            paymentAccountMapperMappers[currency]?.map(response)
                ?: throw IllegalStateException("Not valid Account returned")
        }

    override fun isEligibleForSimpleBuy(): Single<Boolean> =
        authenticator.authenticate {
            nabuService.isEligibleForSimpleBuy(it)
        }.map {
            it.eligible
        }.onErrorReturn {
            false
        }

    override fun isCurrencySupportedForSimpleBuy(fiatCurrency: String): Single<Boolean> =
        nabuService.getSupportedCurrencies(fiatCurrency).map {
            it.pairs.firstOrNull { it.pair.split("-")[1] == fiatCurrency } != null
        }.onErrorReturn { false }

    override fun getOutstandingBuyOrders(): Single<BuyOrderList> =
        authenticator.authenticate {
            nabuService.getOutstandingBuyOrders(it)
        }.map {
            it.map { order -> order.toBuyOrder() }
        }

    override fun getBuyOrder(orderId: String): Single<BuyOrder> =
        authenticator.authenticate {
            nabuService.getBuyOrder(it, orderId)
        }.map { it.toBuyOrder() }

    override fun deleteBuyOrder(orderId: String): Completable =
        authenticator.authenticateCompletable {
            nabuService.deleteBuyOrder(it, orderId)
        }

    override fun getBalanceForAsset(crypto: CryptoCurrency): Maybe<CryptoValue> =
        authenticator.authenticateMaybe {
            nabuService.getBalanceForAsset(it, crypto)
                .map { balance ->
                    CryptoValue.fromMinor(crypto, balance.available.toBigDecimal())
                }
        }

    override fun transferFundsToWallet(amount: CryptoValue, walletAddress: String): Completable =
        authenticator.authenticateCompletable {
            nabuService.transferFunds(
                it,
                TransferRequest(
                    address = walletAddress,
                    currency = amount.currency.symbol,
                    amount = amount.amount.toString()
                )
            )
        }

    override fun cancelAllPendingBuys(): Completable {
        return getOutstandingBuyOrders().toObservable()
            .flatMapIterable()
            .flatMapCompletable { deleteBuyOrder(it.id) }
    }
}

private fun OrderStateResponse.toLocalState(): OrderState =
    when (this) {
        OrderStateResponse.PENDING_DEPOSIT -> OrderState.AWAITING_FUNDS
        OrderStateResponse.FINISHED -> OrderState.FINISHED
        OrderStateResponse.PENDING_EXECUTION,
        OrderStateResponse.DEPOSIT_MATCHED -> OrderState.PENDING
        OrderStateResponse.FAILED,
        OrderStateResponse.EXPIRED -> OrderState.FAILED
        OrderStateResponse.CANCELED -> OrderState.CANCELED
    }

private fun BuyOrderResponse.toBuyOrder(): BuyOrder =
    BuyOrder(
        id = id,
        pair = pair,
        fiat = FiatValue.fromMinor(inputCurrency, inputQuantity.toLongOrDefault(0)),
        crypto = CryptoValue.fromMinor(
            CryptoCurrency.fromSymbol(outputCurrency)
                ?: throw UnknownFormatConversionException("Unknown Crypto currency: $outputCurrency"),
            outputQuantity.toBigDecimalOrNull() ?: BigDecimal.ZERO
        ),
        state = state.toLocalState(),
        expires = expiresAt
    )

interface PaymentAccountMapper {
    fun map(bankAccountResponse: BankAccountResponse): BankAccount?
}
