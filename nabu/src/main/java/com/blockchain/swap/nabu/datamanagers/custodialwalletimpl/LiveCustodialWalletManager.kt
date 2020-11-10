package com.blockchain.swap.nabu.datamanagers.custodialwalletimpl

import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.swap.nabu.Authenticator
import com.blockchain.swap.nabu.datamanagers.BankAccount
import com.blockchain.swap.nabu.datamanagers.BillingAddress
import com.blockchain.swap.nabu.datamanagers.BuyOrderList
import com.blockchain.swap.nabu.datamanagers.BuySellLimits
import com.blockchain.swap.nabu.datamanagers.BuySellOrder
import com.blockchain.swap.nabu.datamanagers.BuySellPair
import com.blockchain.swap.nabu.datamanagers.BuySellPairs
import com.blockchain.swap.nabu.datamanagers.CardToBeActivated
import com.blockchain.swap.nabu.datamanagers.CurrencyPair
import com.blockchain.swap.nabu.datamanagers.CustodialQuote
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.EveryPayCredentials
import com.blockchain.swap.nabu.datamanagers.FiatTransaction
import com.blockchain.swap.nabu.datamanagers.InterestAccountDetails
import com.blockchain.swap.nabu.datamanagers.InterestActivityItem
import com.blockchain.swap.nabu.datamanagers.LinkedBank
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.swap.nabu.datamanagers.Partner
import com.blockchain.swap.nabu.datamanagers.PartnerCredentials
import com.blockchain.swap.nabu.datamanagers.PaymentLimits
import com.blockchain.swap.nabu.datamanagers.PaymentMethod
import com.blockchain.swap.nabu.datamanagers.Product
import com.blockchain.swap.nabu.datamanagers.TransferDirection
import com.blockchain.swap.nabu.datamanagers.SwapLimits
import com.blockchain.swap.nabu.datamanagers.SwapOrder
import com.blockchain.swap.nabu.datamanagers.SwapOrderState
import com.blockchain.swap.nabu.datamanagers.TransactionState
import com.blockchain.swap.nabu.datamanagers.TransactionType
import com.blockchain.swap.nabu.datamanagers.featureflags.Feature
import com.blockchain.swap.nabu.datamanagers.featureflags.FeatureEligibility
import com.blockchain.swap.nabu.datamanagers.repositories.AssetBalancesRepository
import com.blockchain.swap.nabu.datamanagers.repositories.interest.Eligibility
import com.blockchain.swap.nabu.datamanagers.repositories.interest.InterestLimits
import com.blockchain.swap.nabu.datamanagers.repositories.interest.InterestRepository
import com.blockchain.swap.nabu.datamanagers.repositories.swap.SwapRepository
import com.blockchain.swap.nabu.datamanagers.repositories.swap.SwapTransactionItem
import com.blockchain.swap.nabu.extensions.fromIso8601ToUtc
import com.blockchain.swap.nabu.extensions.toLocalTime
import com.blockchain.swap.nabu.models.cards.CardResponse
import com.blockchain.swap.nabu.models.cards.PaymentMethodResponse
import com.blockchain.swap.nabu.models.cards.PaymentMethodsResponse
import com.blockchain.swap.nabu.models.interest.InterestAccountDetailsResponse
import com.blockchain.swap.nabu.models.interest.InterestActivityItemResponse
import com.blockchain.swap.nabu.models.interest.InterestRateResponse
import com.blockchain.swap.nabu.models.nabu.AddAddressRequest
import com.blockchain.swap.nabu.models.nabu.State
import com.blockchain.swap.nabu.models.simplebuy.AddNewCardBodyRequest
import com.blockchain.swap.nabu.models.simplebuy.AmountResponse
import com.blockchain.swap.nabu.models.simplebuy.BankAccountResponse
import com.blockchain.swap.nabu.models.simplebuy.BuyOrderListResponse
import com.blockchain.swap.nabu.models.simplebuy.BuySellOrderResponse
import com.blockchain.swap.nabu.models.simplebuy.CardPartnerAttributes
import com.blockchain.swap.nabu.models.simplebuy.ConfirmOrderRequestBody
import com.blockchain.swap.nabu.models.simplebuy.CustodialWalletOrder
import com.blockchain.swap.nabu.models.simplebuy.TransactionResponse
import com.blockchain.swap.nabu.models.simplebuy.TransferRequest
import com.blockchain.swap.nabu.models.swap.CreateOrderRequest
import com.blockchain.swap.nabu.models.swap.SwapOrderResponse
import com.blockchain.swap.nabu.service.NabuService
import com.braintreepayments.cardform.utils.CardType
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.flatMapIterable
import io.reactivex.rxkotlin.zipWith
import okhttp3.internal.toLongOrDefault
import java.math.BigInteger
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
    private val assetBalancesRepository: AssetBalancesRepository,
    private val interestRepository: InterestRepository,
    private val swapRepository: SwapRepository
) : CustodialWalletManager {

    override fun getQuote(
        cryptoCurrency: CryptoCurrency,
        fiatCurrency: String,
        action: String,
        currency: String,
        amount: String
    ): Single<CustodialQuote> =
        authenticator.authenticate {
            nabuService.getSimpleBuyQuote(
                sessionToken = it,
                action = action,
                currencyPair = "${cryptoCurrency.networkTicker}-$fiatCurrency",
                currency = currency,
                amount = amount
            )
        }.map { quoteResponse ->
            val amountCrypto = CryptoValue.fromMajor(cryptoCurrency,
                (amount.toBigInteger().toFloat().div(quoteResponse.rate)).toBigDecimal())
            CustodialQuote(
                date = quoteResponse.time.toLocalTime(),
                fee = FiatValue.fromMinor(fiatCurrency,
                    quoteResponse.fee.times(amountCrypto.toBigInteger().toLong())),
                estimatedAmount = amountCrypto,
                rate = FiatValue.fromMinor(fiatCurrency, quoteResponse.rate)
            )
        }

    override fun createOrder(
        custodialWalletOrder: CustodialWalletOrder,
        stateAction: String?
    ): Single<BuySellOrder> =
        authenticator.authenticate {
            nabuService.createOrder(
                it,
                custodialWalletOrder,
                stateAction
            )
        }.map { response -> response.toBuySellOrder() }

    override fun createWithdrawOrder(amount: FiatValue, bankId: String): Completable =
        authenticator.authenticateCompletable {
            nabuService.createWithdrawOrder(
                sessionToken = it,
                amount = amount.toBigInteger().toString(),
                currency = amount.currencyCode,
                beneficiaryId = bankId
            )
        }

    override fun fetchWithdrawFee(currency: String): Single<FiatValue> =
        authenticator.authenticate {
            nabuService.fetchWithdrawFee(it)
        }.map { response ->
            response.fees.firstOrNull { it.symbol == currency }?.let {
                FiatValue.fromMajor(it.symbol, it.value)
            } ?: FiatValue.zero(currency)
        }

    override fun fetchWithdrawLocksTime(paymentMethodType: PaymentMethodType): Single<BigInteger> =
        authenticator.authenticate {
            nabuService.fetchWithdrawLocksRules(it, paymentMethodType)
        }.flatMap { response ->
            response.rule?.let {
                Single.just(it.lockTime.toBigInteger())
            } ?: Single.just(BigInteger.ZERO)
        }

    override fun getSupportedBuySellCryptoCurrencies(
        fiatCurrency: String?
    ): Single<BuySellPairs> =
        nabuService.getSupportedCurrencies(fiatCurrency)
            .map {
                val supportedPairs = it.pairs.filter { pair ->
                    pair.isCryptoCurrencySupported()
                }
                BuySellPairs(supportedPairs.map { pair ->
                    BuySellPair(
                        pair = pair.pair,
                        buyLimits = BuySellLimits(
                            pair.buyMin,
                            pair.buyMax
                        ),
                        sellLimits = BuySellLimits(
                            pair.sellMin,
                            pair.sellMax
                        )
                    )
                })
            }

    override fun getSupportedFiatCurrencies(): Single<List<String>> =
        authenticator.authenticate {
            nabuService.getSupportedCurrencies()
        }.map {
            it.pairs.map { pair ->
                pair.pair.split("-")[1]
            }.distinct()
        }

    override fun getTransactions(currency: String): Single<List<FiatTransaction>> =
        authenticator.authenticate { token ->
            nabuService.getTransactions(token, currency).map { response ->
                response.items.map {
                    FiatTransaction(
                        id = it.id,
                        amount = it.amount.toFiat(),
                        date = it.insertedAt.fromIso8601ToUtc() ?: Date(),
                        state = it.state.toTransactionState(),
                        type = it.type.toTransactionType()
                    )
                }
                    .filter { it.state != TransactionState.UNKNOWN && it.type != TransactionType.UNKNOWN }
            }
        }

    private fun AmountResponse.toFiat() =
        FiatValue.fromMajor(symbol, value)

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

    override fun getCustodialAccountAddress(cryptoCurrency: CryptoCurrency): Single<String> =
        authenticator.authenticate {
            nabuService.getSimpleBuyBankAccountDetails(it, cryptoCurrency.networkTicker)
        }.map { response ->
            response.address
        }

    override fun isEligibleForSimpleBuy(fiatCurrency: String): Single<Boolean> =
        authenticator.authenticate {
            nabuService.isEligibleForSimpleBuy(it, fiatCurrency, PAYMENT_METHODS)
        }.map {
            it.simpleBuyTradingEligible
        }.onErrorReturn {
            false
        }

    override fun isCurrencySupportedForSimpleBuy(fiatCurrency: String): Single<Boolean> =
        nabuService.getSupportedCurrencies(fiatCurrency).map {
            it.pairs.firstOrNull { it.pair.split("-")[1] == fiatCurrency } != null
        }.onErrorReturn { false }

    override fun getOutstandingBuyOrders(crypto: CryptoCurrency): Single<BuyOrderList> =
        authenticator.authenticate {
            nabuService.getOutstandingOrders(
                sessionToken = it,
                pendingOnly = true
            )
        }.map {
            it.filterAndMapToOrder(crypto)
        }

    override fun getAllOutstandingBuyOrders(): Single<BuyOrderList> =
        authenticator.authenticate {
            nabuService.getOutstandingOrders(
                sessionToken = it,
                pendingOnly = true
            )
        }.map {
            it.filter { order -> order.type() == OrderType.BUY }.map { order -> order.toBuySellOrder() }
                .filter { order -> order.state != OrderState.UNKNOWN }
        }

    override fun getAllOutstandingOrders(): Single<BuyOrderList> =
        authenticator.authenticate {
            nabuService.getOutstandingOrders(
                sessionToken = it,
                pendingOnly = true
            )
        }.map {
            it.map { order -> order.toBuySellOrder() }
                .filter { order -> order.state != OrderState.UNKNOWN }
        }

    override fun getAllOrdersFor(crypto: CryptoCurrency): Single<BuyOrderList> =
        authenticator.authenticate {
            nabuService.getOutstandingOrders(
                sessionToken = it,
                pendingOnly = false
            )
        }.map {
            it.filterAndMapToOrder(crypto)
        }

    private fun BuyOrderListResponse.filterAndMapToOrder(crypto: CryptoCurrency): List<BuySellOrder> =
        this.filter { order ->
            order.outputCurrency == crypto.networkTicker ||
                    order.inputCurrency == crypto.networkTicker
        }
            .map { order -> order.toBuySellOrder() }

    override fun getBuyOrder(orderId: String): Single<BuySellOrder> =
        authenticator.authenticate {
            nabuService.getBuyOrder(it, orderId)
        }.map { it.toBuySellOrder() }

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

    override fun getTotalBalanceForAsset(crypto: CryptoCurrency): Maybe<CryptoValue> =
        kycFeatureEligibility.isEligibleFor(Feature.SIMPLEBUY_BALANCE)
            .flatMapMaybe { eligible ->
                if (eligible) {
                    assetBalancesRepository.getTotalBalanceForAsset(crypto)
                } else {
                    Maybe.empty()
                }
            }

    override fun getActionableBalanceForAsset(crypto: CryptoCurrency): Maybe<CryptoValue> =
        kycFeatureEligibility.isEligibleFor(Feature.SIMPLEBUY_BALANCE)
            .flatMapMaybe { eligible ->
                if (eligible) {
                    assetBalancesRepository.getActionableBalanceForAsset(crypto)
                } else {
                    Maybe.empty()
                }
            }

    override fun getPendingBalanceForAsset(crypto: CryptoCurrency): Maybe<CryptoValue> =
        assetBalancesRepository.getPendingBalanceForAsset(crypto)

    override fun transferFundsToWallet(amount: CryptoValue, walletAddress: String): Single<String> =
        authenticator.authenticate {
            nabuService.transferFunds(
                it,
                TransferRequest(
                    address = walletAddress,
                    currency = amount.currency.networkTicker,
                    amount = amount.toBigInteger().toString()
                )
            )
        }

    override fun cancelAllPendingOrders(): Completable {
        return getAllOutstandingOrders().toObservable()
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
                    account = beneficiary.address.replace("*", ""),
                    currency = beneficiary.currency
                )
            }
        }

    override fun fetchSuggestedPaymentMethod(
        fiatCurrency: String,
        isTier2Approved: Boolean
    ): Single<List<PaymentMethod>> =
        cardsPaymentFeatureFlag.enabled.zipWith(fundsFeatureFlag.enabled)
            .flatMap { (cardsEnabled, fundsEnabled) ->
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
            assetBalancesRepository.getTotalBalanceForAsset(fiatCurrency)
                .map { balance -> CustodialFiatBalance(fiatCurrency, true, balance) }
                .toSingle(CustodialFiatBalance(fiatCurrency, false, null)),
            nabuService.getCards(it).onErrorReturn { emptyList() },
            nabuService.getPaymentMethods(it, fiatCurrency, isTier2Approved).doOnSuccess {
                updateSupportedCards(it)
            })
    }.map { (custodialFiatBalance, cardsResponse, paymentMethods) ->
        val availablePaymentMethods = mutableListOf<PaymentMethod>()

        paymentMethods.methods.forEach {
            if (it.type == PaymentMethodResponse.PAYMENT_CARD && cardsEnabled) {
                val cardLimits = PaymentLimits(it.limits.min, it.limits.max, fiatCurrency)
                cardsResponse.takeIf { cards -> cards.isNotEmpty() }?.filter { it.state.isActive() }
                    ?.forEach { cardResponse: CardResponse ->
                        availablePaymentMethods.add(cardResponse.toCardPaymentMethod(cardLimits))
                    }
            } else if (
                it.type == PaymentMethodResponse.FUNDS &&
                it.currency == fiatCurrency &&
                SUPPORTED_FUNDS_CURRENCIES.contains(it.currency) &&
                fundsEnabled
            ) {
                custodialFiatBalance.balance?.takeIf { balance ->
                    balance > FiatValue.fromMinor(
                        it.currency,
                        it.limits.min
                    )
                }?.let { balance ->
                    val fundsLimits =
                        PaymentLimits(it.limits.min,
                            it.limits.max.coerceAtMost(balance.toBigInteger().toLong()),
                            it.currency)
                    availablePaymentMethods.add(PaymentMethod.Funds(
                        balance,
                        it.currency,
                        fundsLimits
                    ))
                }
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
        }

        if (!availablePaymentMethods.any {
                it is PaymentMethod.Card || it is PaymentMethod.Funds
            }) {
            availablePaymentMethods.add(PaymentMethod.Undefined)
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
    ): Single<BuySellOrder> =
        authenticator.authenticate {
            nabuService.confirmOrder(it, orderId,
                ConfirmOrderRequestBody(
                    attributes = attributes
                ))
        }.map {
            it.toBuySellOrder()
        }

    override fun getInterestAccountRates(crypto: CryptoCurrency): Single<Double> =
        authenticator.authenticate { sessionToken ->
            nabuService.getInterestRates(sessionToken, crypto.networkTicker).toSingle(InterestRateResponse(0.0))
                .flatMap {
                    Single.just(it.rate)
                }
        }

    override fun getInterestAccountBalance(
        crypto: CryptoCurrency
    ): Maybe<CryptoValue> =
        authenticator.authenticateMaybe { sessionToken ->
            nabuService.getInterestAccountBalance(sessionToken, crypto.networkTicker)
                .map { accountDetailsResponse ->
                    CryptoValue.fromMinor(
                        currency = crypto,
                        minor = accountDetailsResponse.balance.toBigInteger()
                    )
                }
        }

    override fun getPendingInterestAccountBalance(
        crypto: CryptoCurrency
    ): Maybe<CryptoValue> =
        authenticator.authenticateMaybe { sessionToken ->
            nabuService.getInterestAccountBalance(sessionToken, crypto.networkTicker)
                .map { accountDetailsResponse ->
                    CryptoValue.fromMinor(
                        currency = crypto,
                        minor = accountDetailsResponse.pendingDeposit.toBigInteger()
                    )
                }
        }

    override fun getInterestAccountDetails(
        crypto: CryptoCurrency
    ): Single<InterestAccountDetails> =
        authenticator.authenticate { sessionToken ->
            nabuService.getInterestAccountDetails(sessionToken, crypto.networkTicker).map {
                it.toInterestAccountDetails(crypto)
            }
        }

    override fun getInterestAccountAddress(crypto: CryptoCurrency): Single<String> =
        authenticator.authenticate { sessionToken ->
            nabuService.getInterestAddress(sessionToken, crypto.networkTicker).map {
                it.accountRef
            }
        }

    override fun getInterestActivity(crypto: CryptoCurrency): Single<List<InterestActivityItem>> =
        kycFeatureEligibility.isEligibleFor(Feature.INTEREST_RATES)
            .onErrorReturnItem(false)
            .flatMap { eligible ->
                if (eligible) {
                    authenticator.authenticate { sessionToken ->
                        nabuService.getInterestActivity(sessionToken, crypto.networkTicker)
                            .map { interestActivityResponse ->
                                interestActivityResponse.items.map {
                                    val cryptoCurrency =
                                        CryptoCurrency.fromNetworkTicker(it.amount.symbol)!!

                                    it.toInterestActivityItem(cryptoCurrency)
                                }
                            }
                    }
                } else {
                    Single.just(emptyList())
                }
            }

    override fun getInterestLimits(crypto: CryptoCurrency): Maybe<InterestLimits> =
        interestRepository.getLimitForAsset(crypto)

    override fun getInterestAvailabilityForAsset(crypto: CryptoCurrency): Single<Boolean> =
        interestRepository.getAvailabilityForAsset(crypto)

    override fun getInterestEnabledAssets(): Single<List<CryptoCurrency>> =
        interestRepository.getAvailableAssets()

    override fun getInterestEligibilityForAsset(crypto: CryptoCurrency): Single<Eligibility> =
        interestRepository.getEligibilityForAsset(crypto)

    override fun getSupportedFundsFiats(
        fiatCurrency: String,
        isTier2Approved: Boolean
    ): Single<List<String>> {

        val supportedFundCurrencies = authenticator.authenticate {
            nabuService.getPaymentMethods(it, fiatCurrency, isTier2Approved)
        }.map { paymentMethodsResponse ->
            paymentMethodsResponse.methods.filter {
                it.type.toPaymentMethodType() == PaymentMethodType.FUNDS &&
                        SUPPORTED_FUNDS_CURRENCIES.contains(it.currency)
            }.mapNotNull {
                it.currency
            }
        }

        return fundsFeatureFlag.enabled.flatMap { enabled ->
            if (enabled) {
                supportedFundCurrencies
            } else {
                Single.just(emptyList())
            }
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

    override fun createSwapOrder(
        direction: TransferDirection,
        quoteId: String,
        volume: Money,
        destinationAddress: String?
    ): Single<SwapOrder> =
        authenticator.authenticate { sessionToken ->
            nabuService.createSwapOrder(
                sessionToken,
                CreateOrderRequest(
                    direction = direction.toString(),
                    quoteId = quoteId,
                    volume = volume.toBigInteger().toString(),
                    destinationAddress = destinationAddress
                )
            ).map {
                it.toSwapOrder() ?: throw java.lang.IllegalStateException("Invalid order created")
            }
        }

    override fun getSwapLimits(currency: String): Single<SwapLimits> =
        authenticator.authenticate {
            nabuService.getSwapLimits(
                it,
                currency
            ).map { response ->
                if (response.maxOrder == null && response.minOrder == null && response.maxPossibleOrder == null) {
                    SwapLimits(currency)
                } else {
                    SwapLimits(
                        minLimit = FiatValue.fromMinor(currency, response.minOrder?.toLong() ?: 0L),
                        maxOrder = FiatValue.fromMinor(currency, response.maxOrder?.toLong() ?: 0L),
                        maxLimit = FiatValue.fromMinor(currency, response.maxPossibleOrder?.toLong() ?: 0L)
                    )
                }
            }
        }

    override fun getSwapActivityForAsset(
        cryptoCurrency: CryptoCurrency,
        directions: List<TransferDirection>
    ): Single<List<SwapTransactionItem>> =
        swapRepository.getSwapActivityForAsset(cryptoCurrency, directions)

    override fun updateSwapOrder(id: String, success: Boolean): Completable =
        authenticator.authenticateCompletable { sessionToken ->
            nabuService.updateOrder(
                sessionToken = sessionToken,
                id = id,
                success = success
            )
        }

    override fun createPendingDeposit(
        crypto: CryptoCurrency,
        address: String,
        hash: String,
        amount: Money,
        product: Product
    ): Completable =
        authenticator.authenticateCompletable { sessionToken ->
            nabuService.createDepositTransaction(
                sessionToken = sessionToken,
                currency = crypto.networkTicker,
                address = address,
                hash = hash,
                amount = amount.toBigInteger().toString(),
                product = product.toString()

            )
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
                    set(it.expireYear ?: this.get(Calendar.YEAR),
                        it.expireMonth ?: this.get(Calendar.MONTH),
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

    override fun getSwapTrades(): Single<List<SwapOrder>> =
        authenticator.authenticate { sessionToken ->
            nabuService.getSwapTrades(sessionToken)
        }.map { response ->
            response.mapNotNull { orderResp ->
                orderResp.toSwapOrder()
            }
        }

    private fun SwapOrderResponse.toSwapOrder(): SwapOrder? {
        return SwapOrder(
            id = this.id,
            state = this.state.toSwapState(),
            depositAddress = this.kind.depositAddress,
            createdAt = this.createdAt.fromIso8601ToUtc() ?: Date(),
            inputMoney = CryptoValue.fromMinor(
                CryptoCurrency.fromNetworkTicker(
                    this.pair.toCryptoCurrencyPair()?.source?.networkTicker.toString()
                ) ?: return null, this.priceFunnel.inputMoney.toBigInteger()
            ),
            outputMoney = CryptoValue.fromMinor(
                CryptoCurrency.fromNetworkTicker(
                    this.pair.toCryptoCurrencyPair()?.destination?.networkTicker.toString()
                ) ?: return null, this.priceFunnel.outputMoney.toBigInteger()
            )
        )
    }

    companion object {
        private const val PAYMENT_METHODS = "BANK_ACCOUNT,PAYMENT_CARD"

        private val SUPPORTED_FUNDS_CURRENCIES = listOf(
            "GBP", "EUR", "USD"
        )
    }
}

private fun String.toCryptoCurrencyPair(): CurrencyPair.CryptoCurrencyPair? {
    val parts = split("-")
    if (parts.size != 2) return null
    val source = CryptoCurrency.fromNetworkTicker(parts[0]) ?: return null
    val destination = CryptoCurrency.fromNetworkTicker(parts[1]) ?: return null
    return CurrencyPair.CryptoCurrencyPair(source, destination)
}

private fun String.toTransactionState(): TransactionState =
    when (this) {
        TransactionResponse.COMPLETE -> TransactionState.COMPLETED
        else -> TransactionState.UNKNOWN
    }

fun String.toSwapState(): SwapOrderState =
    when (this) {
        SwapOrderResponse.CREATED -> SwapOrderState.CREATED
        SwapOrderResponse.PENDING_CONFIRMATION -> SwapOrderState.PENDING_CONFIRMATION
        SwapOrderResponse.PENDING_EXECUTION -> SwapOrderState.PENDING_EXECUTION
        SwapOrderResponse.PENDING_DEPOSIT -> SwapOrderState.PENDING_DEPOSIT
        SwapOrderResponse.PENDING_LEDGER -> SwapOrderState.PENDING_LEDGER
        SwapOrderResponse.FINISH_DEPOSIT -> SwapOrderState.FINISH_DEPOSIT
        SwapOrderResponse.PENDING_WITHDRAWAL -> SwapOrderState.PENDING_WITHDRAWAL
        SwapOrderResponse.EXPIRED -> SwapOrderState.EXPIRED
        SwapOrderResponse.FINISHED -> SwapOrderState.FINISHED
        SwapOrderResponse.CANCELED -> SwapOrderState.CANCELED
        SwapOrderResponse.FAILED -> SwapOrderState.FAILED
        else -> SwapOrderState.UNKNOWN
    }

private fun String.toTransactionType(): TransactionType =
    when (this) {
        TransactionResponse.DEPOSIT -> TransactionType.DEPOSIT
        TransactionResponse.WITHDRAWAL -> TransactionType.WITHDRAWAL
        else -> TransactionType.UNKNOWN
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
        BuySellOrderResponse.PENDING_DEPOSIT -> OrderState.AWAITING_FUNDS
        BuySellOrderResponse.FINISHED -> OrderState.FINISHED
        BuySellOrderResponse.PENDING_CONFIRMATION -> OrderState.PENDING_CONFIRMATION
        BuySellOrderResponse.PENDING_EXECUTION,
        BuySellOrderResponse.DEPOSIT_MATCHED -> OrderState.PENDING_EXECUTION
        BuySellOrderResponse.FAILED,
        BuySellOrderResponse.EXPIRED -> OrderState.FAILED
        BuySellOrderResponse.CANCELED -> OrderState.CANCELED
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

private fun BuySellOrderResponse.type() =
    when (side) {
        "BUY" -> OrderType.BUY
        "SELL" -> OrderType.SELL
        else -> throw IllegalStateException("Unsupported order type")
    }

enum class OrderType {
    BUY,
    SELL
}

private fun BuySellOrderResponse.toBuySellOrder(): BuySellOrder {
    val fiatCurrency = if (type() == OrderType.BUY) inputCurrency else outputCurrency
    val cryptoCurrency =
        CryptoCurrency.fromNetworkTicker(if (type() == OrderType.BUY) outputCurrency else inputCurrency)
            ?: throw UnknownFormatConversionException("Unknown Crypto currency: $inputCurrency")
    val fiatAmount =
        if (type() == OrderType.BUY) inputQuantity.toLongOrDefault(0) else outputQuantity.toLongOrDefault(0)

    val cryptoAmount =
        (if (type() == OrderType.BUY) outputQuantity.toBigInteger() else inputQuantity.toBigInteger())

    return BuySellOrder(
        id = id,
        pair = pair,
        fiat = FiatValue.fromMinor(fiatCurrency, fiatAmount),
        crypto = CryptoValue.fromMinor(cryptoCurrency, cryptoAmount),
        state = state.toLocalState(),
        expires = expiresAt.fromIso8601ToUtc() ?: Date(0),
        updated = updatedAt.fromIso8601ToUtc() ?: Date(0),
        created = insertedAt.fromIso8601ToUtc() ?: Date(0),
        fee = fee?.let {
            FiatValue.fromMinor(fiatCurrency, it.toLongOrDefault(0))
        },
        paymentMethodId = paymentMethodId ?: (
                when (paymentType.toPaymentMethodType()) {
                    PaymentMethodType.BANK_ACCOUNT -> PaymentMethod.BANK_PAYMENT_ID
                    PaymentMethodType.FUNDS -> PaymentMethod.FUNDS_PAYMENT_ID
                    else -> PaymentMethod.UNDEFINED_CARD_PAYMENT_ID
                }),
        paymentMethodType = paymentType.toPaymentMethodType(),
        price = price?.let {
            FiatValue.fromMinor(fiatCurrency, it.toLong())
        },
        orderValue = if (type() == OrderType.BUY)
            CryptoValue.fromMinor(cryptoCurrency, cryptoAmount)
        else
            FiatValue.fromMinor(outputCurrency, outputQuantity.toLongOrDefault(0)),
        attributes = attributes,
        type = type()
    )
}

private fun String.toPaymentMethodType(): PaymentMethodType =
    when (this) {
        PaymentMethodResponse.BANK_ACCOUNT -> PaymentMethodType.BANK_ACCOUNT
        PaymentMethodResponse.PAYMENT_CARD -> PaymentMethodType.PAYMENT_CARD
        PaymentMethodResponse.FUNDS -> PaymentMethodType.FUNDS
        else -> PaymentMethodType.UNKNOWN
    }

private fun InterestActivityItemResponse.toInterestActivityItem(cryptoCurrency: CryptoCurrency) =
    InterestActivityItem(
        value = CryptoValue.fromMinor(cryptoCurrency, amountMinor.toBigInteger()),
        cryptoCurrency = cryptoCurrency,
        id = id,
        insertedAt = insertedAt.fromIso8601ToUtc() ?: Date(0),
        state = InterestActivityItem.toInterestState(state),
        type = InterestActivityItem.toTransactionType(type),
        extraAttributes = extraAttributes
    )

private fun InterestAccountDetailsResponse.toInterestAccountDetails(cryptoCurrency: CryptoCurrency) =
    InterestAccountDetails(
        balance = CryptoValue.fromMinor(cryptoCurrency, balance.toBigInteger()),
        pendingInterest = CryptoValue.fromMinor(cryptoCurrency, pendingInterest.toBigInteger()),
        totalInterest = CryptoValue.fromMinor(cryptoCurrency, totalInterest.toBigInteger())
    )

interface PaymentAccountMapper {
    fun map(bankAccountResponse: BankAccountResponse): BankAccount?
}

private data class CustodialFiatBalance(
    val currency: String,
    val available: Boolean,
    val balance: FiatValue?
)