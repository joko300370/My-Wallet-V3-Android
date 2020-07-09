package com.blockchain.swap.nabu.datamanagers.custodialwalletimpl

import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.swap.nabu.Authenticator
import com.blockchain.swap.nabu.datamanagers.BankAccount
import com.blockchain.swap.nabu.datamanagers.BillingAddress
import com.blockchain.swap.nabu.datamanagers.BuyLimits
import com.blockchain.swap.nabu.datamanagers.BuyOrder
import com.blockchain.swap.nabu.datamanagers.BuyOrderList
import com.blockchain.swap.nabu.datamanagers.CardToBeActivated
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.EveryPayCredentials
import com.blockchain.swap.nabu.datamanagers.LinkedBank
import com.blockchain.swap.nabu.datamanagers.OrderInput
import com.blockchain.swap.nabu.datamanagers.OrderOutput
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.swap.nabu.datamanagers.Partner
import com.blockchain.swap.nabu.datamanagers.PartnerCredentials
import com.blockchain.swap.nabu.datamanagers.PaymentLimits
import com.blockchain.swap.nabu.datamanagers.PaymentMethod
import com.blockchain.swap.nabu.datamanagers.Quote
import com.blockchain.swap.nabu.datamanagers.SimpleBuyPair
import com.blockchain.swap.nabu.datamanagers.SimpleBuyPairs
import com.blockchain.swap.nabu.datamanagers.featureflags.Feature
import com.blockchain.swap.nabu.datamanagers.featureflags.FeatureEligibility
import com.blockchain.swap.nabu.datamanagers.repositories.AssetBalancesRepository
import com.blockchain.swap.nabu.extensions.fromIso8601ToUtc
import com.blockchain.swap.nabu.extensions.toLocalTime
import com.blockchain.swap.nabu.models.cards.CardResponse
import com.blockchain.swap.nabu.models.cards.PaymentMethodResponse
import com.blockchain.swap.nabu.models.cards.PaymentMethodsResponse
import com.blockchain.swap.nabu.models.nabu.AddAddressRequest
import com.blockchain.swap.nabu.models.nabu.State
import com.blockchain.swap.nabu.models.simplebuy.AddNewCardBodyRequest
import com.blockchain.swap.nabu.models.simplebuy.BankAccountResponse
import com.blockchain.swap.nabu.models.simplebuy.BuyOrderListResponse
import com.blockchain.swap.nabu.models.simplebuy.BuyOrderResponse
import com.blockchain.swap.nabu.models.simplebuy.CardPartnerAttributes
import com.blockchain.swap.nabu.models.simplebuy.ConfirmOrderRequestBody
import com.blockchain.swap.nabu.models.simplebuy.CustodialWalletOrder
import com.blockchain.swap.nabu.models.simplebuy.TransferRequest
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.swap.nabu.service.NabuService
import com.braintreepayments.cardform.utils.CardType
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.flatMapIterable
import io.reactivex.rxkotlin.zipWith
import okhttp3.internal.toLongOrDefault
import java.math.BigDecimal
import java.util.Calendar
import java.util.Date
import java.util.UnknownFormatConversionException

class LiveCustodialWalletManager(
    private val nabuService: NabuService,
    private val authenticator: Authenticator,
    private val simpleBuyPrefs: SimpleBuyPrefs,
    private val cardsPaymentFeatureFlag: FeatureFlag,
    private val fundsFeatureFlag: FeatureFlag,
    private val paymentAccountMapperMappers: Map<String, PaymentAccountMapper>,
    private val kycFeatureEligibility: FeatureEligibility,
    private val assetBalancesRepository: AssetBalancesRepository
) : CustodialWalletManager {

    override fun getQuote(
        action: String,
        crypto: CryptoCurrency,
        amount: FiatValue
    ): Single<Quote> =
        authenticator.authenticate {
            nabuService.getSimpleBuyQuote(
                sessionToken = it,
                action = action,
                currencyPair = "${crypto.networkTicker}-${amount.currencyCode}",
                amount = amount.valueMinor.toString()
            )
        }.map { quoteResponse ->
            val amountCrypto = CryptoValue.fromMajor(crypto,
                (amount.valueMinor.toFloat().div(quoteResponse.rate)).toBigDecimal())
            Quote(
                date = quoteResponse.time.toLocalTime(),
                fee = FiatValue.fromMinor(amount.currencyCode,
                    quoteResponse.fee.times(amountCrypto.amount.toLong())),
                estimatedAmount = amountCrypto,
                rate = FiatValue.fromMinor(amount.currencyCode, quoteResponse.rate)
            )
        }

    override fun createOrder(
        cryptoCurrency: CryptoCurrency,
        amount: FiatValue,
        action: String,
        paymentMethodId: String?,
        paymentMethodType: PaymentMethodType,
        stateAction: String?
    ): Single<BuyOrder> =
        authenticator.authenticate {
            nabuService.createOrder(
                it,
                CustodialWalletOrder(
                    pair = "${cryptoCurrency.networkTicker}-${amount.currencyCode}",
                    action = action,
                    input = OrderInput(
                        amount.currencyCode, amount.valueMinor.toString()
                    ),
                    output = OrderOutput(
                        cryptoCurrency.networkTicker
                    ),
                    paymentMethodId = paymentMethodId,
                    paymentType = paymentMethodType.name
                ),
                stateAction
            )
        }.map { response -> response.toBuyOrder() }

    override fun getBuyLimitsAndSupportedCryptoCurrencies(
        nabuOfflineTokenResponse: NabuOfflineTokenResponse,
        fiatCurrency: String
    ): Single<SimpleBuyPairs> =
        authenticator.authenticate {
            nabuService.getSupportedCurrencies(fiatCurrency)
        }.map {
            val supportedPairs = it.pairs.filter { pair ->
                pair.isCryptoCurrencySupported()
            }
            SimpleBuyPairs(supportedPairs.map { pair ->
                SimpleBuyPair(
                    pair.pair,
                    BuyLimits(
                        pair.buyMin,
                        pair.buyMax
                    )
                )
            })
        }

    override fun getSupportedFiatCurrencies(
        nabuOfflineTokenResponse: NabuOfflineTokenResponse
    ): Single<List<String>> =
        authenticator.authenticate {
            nabuService.getSupportedCurrencies()
        }.map {
            it.pairs.map { pair ->
                pair.pair.split("-")[1]
            }.distinct()
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

    override fun isEligibleForSimpleBuy(fiatCurrency: String): Single<Boolean> =
        authenticator.authenticate {
            nabuService.isEligibleForSimpleBuy(it, fiatCurrency, PAYMENT_METHODS)
        }.map {
            it.eligible
        }.onErrorReturn {
            false
        }

    override fun isCurrencySupportedForSimpleBuy(fiatCurrency: String): Single<Boolean> =
        nabuService.getSupportedCurrencies(fiatCurrency).map {
            it.pairs.firstOrNull { it.pair.split("-")[1] == fiatCurrency } != null
        }.onErrorReturn { false }

    override fun getOutstandingBuyOrders(crypto: CryptoCurrency): Single<BuyOrderList> =
        authenticator.authenticate {
            nabuService.getOutstandingBuyOrders(
                sessionToken = it,
                pendingOnly = true
            )
        }.map {
            it.filterAndMapToOrder(crypto)
        }

    override fun getAllOutstandingBuyOrders(): Single<BuyOrderList> =
        authenticator.authenticate {
            nabuService.getOutstandingBuyOrders(
                sessionToken = it,
                pendingOnly = true
            )
        }.map {
            it.map { order -> order.toBuyOrder() }
                .filter { order -> order.state != OrderState.UNKNOWN }
        }

    override fun getAllBuyOrdersFor(crypto: CryptoCurrency): Single<BuyOrderList> =
        authenticator.authenticate {
            nabuService.getOutstandingBuyOrders(
                sessionToken = it,
                pendingOnly = false
            )
        }.map {
            it.filterAndMapToOrder(crypto)
        }

    private fun BuyOrderListResponse.filterAndMapToOrder(crypto: CryptoCurrency): List<BuyOrder> =
        this.filter { order -> order.outputCurrency == crypto.networkTicker }
            .map { order -> order.toBuyOrder() }

    override fun getBuyOrder(orderId: String): Single<BuyOrder> =
        authenticator.authenticate {
            nabuService.getBuyOrder(it, orderId)
        }.map { it.toBuyOrder() }

    override fun deleteBuyOrder(orderId: String): Completable =
        authenticator.authenticateCompletable {
            nabuService.deleteBuyOrder(it, orderId)
        }

    override fun deleteCard(cardId: String): Completable =
        authenticator.authenticateCompletable {
            nabuService.deleteCard(it, cardId)
        }

    override fun deleteBank(bankId: String): Completable =
        authenticator.authenticateCompletable {
            nabuService.deleteBank(it, bankId)
        }

    override fun getBalanceForAsset(crypto: CryptoCurrency): Maybe<CryptoValue> =
        kycFeatureEligibility.isEligibleFor(Feature.SIMPLEBUY_BALANCE)
            .flatMapMaybe { eligible ->
                if (eligible) {
                    assetBalancesRepository.getBalanceForAsset(crypto)
                } else {
                    Maybe.empty()
                }
            }

    override fun transferFundsToWallet(amount: CryptoValue, walletAddress: String): Completable =
        authenticator.authenticateCompletable {
            nabuService.transferFunds(
                it,
                TransferRequest(
                    address = walletAddress,
                    currency = amount.currency.networkTicker,
                    amount = amount.amount.toString()
                )
            )
        }

    override fun cancelAllPendingBuys(): Completable {
        return getAllOutstandingBuyOrders().toObservable()
            .flatMapIterable()
            .flatMapCompletable { deleteBuyOrder(it.id) }
    }

    override fun updateSupportedCardTypes(
        fiatCurrency: String,
        isTier2Approved: Boolean
    ): Completable =
        authenticator.authenticate {
            nabuService.getPaymentMethods(it, fiatCurrency, isTier2Approved).doOnSuccess {
                updateSupportedCards(it)
            }
        }.ignoreElement()

    override fun getLinkedBanks(): Single<List<LinkedBank>> =
        authenticator.authenticate {
            nabuService.getLinkedBanks(it)
        }.map {
            it.map { beneficiary ->
                LinkedBank(
                    id = beneficiary.id,
                    title = "${beneficiary.name} ${beneficiary.agent.account}",
                    // address is returned from the api as ****6810
                    account = beneficiary.address.removePrefix("****"),
                    currency = beneficiary.currency
                )
            }
        }

    override fun fetchSuggestedPaymentMethod(
        fiatCurrency: String,
        isTier2Approved: Boolean
    ): Single<List<PaymentMethod>> =
        cardsPaymentFeatureFlag.enabled.zipWith(fundsFeatureFlag.enabled).flatMap { (cardsEnabled, fundsEnabled) ->
            paymentMethods(cardsEnabled, fundsEnabled, fiatCurrency, isTier2Approved)
        }

    private val updateSupportedCards: (PaymentMethodsResponse) -> Unit = {
        val cardTypes = it.methods.filter { it.subTypes.isNullOrEmpty().not() }.mapNotNull {
            it.subTypes
        }.flatten().distinct()
        simpleBuyPrefs.updateSupportedCards(cardTypes.joinToString())
    }

    private fun paymentMethods(
        cardsEnabled: Boolean,
        fundsEnabled: Boolean,
        fiatCurrency: String,
        isTier2Approved: Boolean
    ) = authenticator.authenticate {
        Singles.zip(
            assetBalancesRepository.getBalanceForAsset(fiatCurrency)
                .toSingle(FiatValue.zero(fiatCurrency)),
            nabuService.getCards(it).onErrorReturn { emptyList() },
            nabuService.getPaymentMethods(it, fiatCurrency, isTier2Approved).doOnSuccess {
                updateSupportedCards(it)
            }
        )
    }.map { (fiatBalance, cardsResponse, paymentMethods) ->
        val availablePaymentMethods = mutableListOf<PaymentMethod>()

        paymentMethods.methods.forEach {
            if (it.type == PaymentMethodResponse.PAYMENT_CARD && cardsEnabled) {
                val cardLimits = PaymentLimits(it.limits.min, it.limits.max, fiatCurrency)
                cardsResponse.takeIf { cards -> cards.isNotEmpty() }?.filter { it.state.isActive() }
                    ?.forEach { cardResponse: CardResponse ->
                        availablePaymentMethods.add(cardResponse.toCardPaymentMethod(cardLimits))
                    }
            } else if (it.type == PaymentMethodResponse.FUNDS &&
                it.currency == fiatCurrency &&
                SUPPORTED_FUNDS_CURRENCIES.contains(it.currency) &&
                fundsEnabled
            ) {
                val fundsLimits =
                    PaymentLimits(it.limits.min,
                        it.limits.max.coerceAtMost(fiatBalance.valueMinor), it.currency)
                availablePaymentMethods.add(PaymentMethod.Funds(
                    fiatBalance,
                    it.currency,
                    fundsLimits
                ))

                availablePaymentMethods.add(PaymentMethod.UndefinedFunds(
                    it.currency,
                    PaymentLimits(it.limits.min, it.limits.max, it.currency)))
            }
        }

        paymentMethods.methods.firstOrNull { paymentMethod ->
            paymentMethod.type == PaymentMethodResponse.PAYMENT_CARD && cardsEnabled
        }?.let {
            availablePaymentMethods.add(PaymentMethod.UndefinedCard(PaymentLimits(it.limits.min,
                it.limits.max,
                fiatCurrency)))
            if (cardsResponse.isEmpty() && isTier2Approved) {
                availablePaymentMethods.add(PaymentMethod.Undefined)
            }
        }

        availablePaymentMethods.sortedBy { it.order }.toList()
    }

    override fun addNewCard(
        fiatCurrency: String,
        billingAddress: BillingAddress
    ): Single<CardToBeActivated> =
        authenticator.authenticate {
            nabuService.addNewCard(sessionToken = it,
                addNewCardBodyRequest = AddNewCardBodyRequest(fiatCurrency,
                    AddAddressRequest.fromBillingAddress(billingAddress)))
        }.map {
            CardToBeActivated(cardId = it.id, partner = it.partner)
        }

    override fun activateCard(
        cardId: String,
        attributes: CardPartnerAttributes
    ): Single<PartnerCredentials> =
        authenticator.authenticate {
            nabuService.activateCard(it, cardId, attributes)
        }.map {
            PartnerCredentials(it.everypay?.let { response ->
                EveryPayCredentials(
                    response.apiUsername,
                    response.mobileToken,
                    response.paymentLink
                )
            })
        }

    override fun getCardDetails(cardId: String): Single<PaymentMethod.Card> =
        authenticator.authenticate {
            nabuService.getCardDetails(it, cardId)
        }.map {
            it.toCardPaymentMethod(
                PaymentLimits(FiatValue.zero(it.currency), FiatValue.zero(it.currency)))
        }

    override fun fetchUnawareLimitsCards(
        states: List<CardStatus>
    ): Single<List<PaymentMethod.Card>> =
        authenticator.authenticate {
            nabuService.getCards(it)
        }.map {
            it.filter { states.contains(it.state.toCardStatus()) || states.isEmpty() }.map {
                it.toCardPaymentMethod(
                    PaymentLimits(FiatValue.zero(it.currency), FiatValue.zero(it.currency)))
            }
        }

    override fun confirmOrder(
        orderId: String,
        attributes: CardPartnerAttributes?
    ): Single<BuyOrder> =
        authenticator.authenticate {
            nabuService.confirmOrder(it, orderId,
                ConfirmOrderRequestBody(
                    attributes = attributes
                ))
        }.map {
            it.toBuyOrder()
        }

    override fun getInterestAccountRates(crypto: CryptoCurrency): Single<Double> =
        kycFeatureEligibility.isEligibleFor(Feature.INTEREST_RATES)
            .onErrorReturnItem(false)
            .flatMap { eligible ->
                if (eligible) {
                    authenticator.authenticate { sessionToken ->
                        nabuService.getInterestRates(sessionToken, crypto.networkTicker).map {
                            it.body()?.rate ?: 0.0
                        }
                    }
                } else {
                    Single.just(0.0)
                }
            }

    override fun getInterestAccountDetails(
        crypto: CryptoCurrency
    ): Maybe<CryptoValue> =
        kycFeatureEligibility.isEligibleFor(Feature.INTEREST_DETAILS)
            .flatMapMaybe { eligible ->
                if (eligible) {
                    authenticator.authenticateMaybe { sessionToken ->
                        nabuService.getInterestAccountBalance(sessionToken, crypto.networkTicker)
                            .map { accountBalanceResponse ->
                                CryptoValue.fromMinor(
                                    currency = crypto,
                                    minor = accountBalanceResponse.balance.toBigInteger()
                                )
                            }
                    }
                } else {
                    Maybe.empty()
                }
            }

    override fun getSupportedFundsFiats(fiatCurrency: String, isTier2Approved: Boolean) =
        fundsFeatureFlag.enabled.flatMap { enabled ->
            if (enabled) {
                authenticator.authenticate {
                    nabuService.getPaymentMethods(it, fiatCurrency, isTier2Approved)
                }.map { paymentMethodsResponse ->
                    paymentMethodsResponse.methods.filter {
                        it.type == PaymentMethodResponse.FUNDS &&
                                SUPPORTED_FUNDS_CURRENCIES.contains(
                                    it.currency)
                    }.mapNotNull {
                        it.currency
                    }
                }
            } else {
                Single.just(emptyList())
            }
        }

    override fun getExchangeSendAddressFor(crypto: CryptoCurrency): Maybe<String> =
        authenticator.authenticateMaybe { sessionToken ->
            nabuService.fetchPitSendToAddressForCrypto(sessionToken, crypto.networkTicker)
                .flatMapMaybe { response ->
                    if (response.state == State.ACTIVE) {
                        Maybe.just(response.address)
                    } else {
                        Maybe.empty()
                    }
                }
                .onErrorComplete()
        }

    private fun CardResponse.toCardPaymentMethod(cardLimits: PaymentLimits) =
        PaymentMethod.Card(
            cardId = id,
            limits = cardLimits,
            label = card?.label ?: "",
            endDigits = card?.number ?: "",
            partner = partner.toSupportedPartner(),
            expireDate = card?.let {
                Calendar.getInstance().apply {
                    set(it.expireYear,
                        it.expireMonth,
                        0)
                }.time
            } ?: Date(),
            cardType = card?.type ?: CardType.UNKNOWN,
            status = state.toCardStatus()
        )

    private fun String.isActive(): Boolean =
        toCardStatus() == CardStatus.ACTIVE

    private fun String.isActiveOrExpired(): Boolean =
        isActive() || toCardStatus() == CardStatus.EXPIRED

    private fun String.toCardStatus(): CardStatus =
        when (this) {
            CardResponse.ACTIVE -> CardStatus.ACTIVE
            CardResponse.BLOCKED -> CardStatus.BLOCKED
            CardResponse.PENDING -> CardStatus.PENDING
            CardResponse.CREATED -> CardStatus.CREATED
            CardResponse.EXPIRED -> CardStatus.EXPIRED
            else -> CardStatus.UNKNOWN
        }

    companion object {
        private const val PAYMENT_METHODS = "BANK_ACCOUNT,PAYMENT_CARD"

        private val SUPPORTED_FUNDS_CURRENCIES = listOf(
            "GBP", "EUR"
        )
    }
}

private fun String.toSupportedPartner(): Partner =
    when (this) {
        "EVERYPAY" -> Partner.EVERYPAY
        else -> Partner.UNKNOWN
    }

enum class PaymentMethodType {
    BANK_ACCOUNT,
    PAYMENT_CARD,
    FUNDS,
    UNKNOWN;

    fun toAnalyticsString() =
        when (this) {
            BANK_ACCOUNT -> "BANK"
            PAYMENT_CARD -> "CARD"
            FUNDS -> "FUNDS"
            else -> ""
        }
}

private fun String.toLocalState(): OrderState =
    when (this) {
        BuyOrderResponse.PENDING_DEPOSIT -> OrderState.AWAITING_FUNDS
        BuyOrderResponse.FINISHED -> OrderState.FINISHED
        BuyOrderResponse.PENDING_CONFIRMATION -> OrderState.PENDING_CONFIRMATION
        BuyOrderResponse.PENDING_EXECUTION,
        BuyOrderResponse.DEPOSIT_MATCHED -> OrderState.PENDING_EXECUTION
        BuyOrderResponse.FAILED,
        BuyOrderResponse.EXPIRED -> OrderState.FAILED
        BuyOrderResponse.CANCELED -> OrderState.CANCELED
        else -> OrderState.UNKNOWN
    }

enum class CardStatus {
    PENDING,
    ACTIVE,
    BLOCKED,
    CREATED,
    UNKNOWN,
    EXPIRED
}

private fun BuyOrderResponse.toBuyOrder(): BuyOrder =
    BuyOrder(
        id = id,
        pair = pair,
        fiat = FiatValue.fromMinor(inputCurrency, inputQuantity.toLongOrDefault(0)),
        crypto = CryptoValue.fromMinor(
            CryptoCurrency.fromNetworkTicker(outputCurrency)
                ?: throw UnknownFormatConversionException(
                    "Unknown Crypto currency: $outputCurrency"),
            outputQuantity.toBigDecimalOrNull() ?: BigDecimal.ZERO
        ),
        state = state.toLocalState(),
        expires = expiresAt.fromIso8601ToUtc() ?: Date(0),
        updated = updatedAt.fromIso8601ToUtc() ?: Date(0),
        created = insertedAt.fromIso8601ToUtc() ?: Date(0),
        fee = fee?.let { FiatValue.fromMinor(inputCurrency, it.toLongOrDefault(0)) },
        paymentMethodId = paymentMethodId ?: (
                when (paymentType.toPaymentMethodType()) {
                    PaymentMethodType.BANK_ACCOUNT -> PaymentMethod.BANK_PAYMENT_ID
                    PaymentMethodType.FUNDS -> PaymentMethod.FUNDS_PAYMENT_ID
                    else -> PaymentMethod.UNDEFINED_CARD_PAYMENT_ID
                }),
        paymentMethodType = paymentType.toPaymentMethodType(),
        price = price?.let {
            FiatValue.fromMinor(
                inputCurrency,
                it.toLong()
            )
        },
        orderValue = outputQuantity.toBigDecimalOrNull()?.let {
            CryptoValue.fromMinor(CryptoCurrency.fromNetworkTicker(outputCurrency)
                ?: throw UnknownFormatConversionException(
                    "Unknown Crypto currency: $outputCurrency"),
                it
            )
        },
        attributes = attributes
    )

private fun String.toPaymentMethodType(): PaymentMethodType =
    when (this) {
        PaymentMethodResponse.BANK_ACCOUNT -> PaymentMethodType.BANK_ACCOUNT
        PaymentMethodResponse.PAYMENT_CARD -> PaymentMethodType.PAYMENT_CARD
        PaymentMethodResponse.FUNDS -> PaymentMethodType.FUNDS
        else -> PaymentMethodType.UNKNOWN
    }

interface PaymentAccountMapper {
    fun map(bankAccountResponse: BankAccountResponse): BankAccount?
}
