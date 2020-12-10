package com.blockchain.swap.nabu.datamanagers.custodialwalletimpl

import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.swap.nabu.Authenticator
import com.blockchain.swap.nabu.datamanagers.BankAccount
import com.blockchain.swap.nabu.datamanagers.Beneficiary
import com.blockchain.swap.nabu.datamanagers.BillingAddress
import com.blockchain.swap.nabu.datamanagers.BuyOrderList
import com.blockchain.swap.nabu.datamanagers.BuySellLimits
import com.blockchain.swap.nabu.datamanagers.BuySellOrder
import com.blockchain.swap.nabu.datamanagers.BuySellPair
import com.blockchain.swap.nabu.datamanagers.BuySellPairs
import com.blockchain.swap.nabu.datamanagers.CardToBeActivated
import com.blockchain.swap.nabu.datamanagers.CurrencyPair
import com.blockchain.swap.nabu.datamanagers.CustodialOrder
import com.blockchain.swap.nabu.datamanagers.CustodialOrderState
import com.blockchain.swap.nabu.datamanagers.CustodialQuote
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.EveryPayCredentials
import com.blockchain.swap.nabu.datamanagers.ExtraAttributesProvider
import com.blockchain.swap.nabu.datamanagers.FiatTransaction
import com.blockchain.swap.nabu.datamanagers.InterestAccountDetails
import com.blockchain.swap.nabu.datamanagers.InterestActivityItem
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.swap.nabu.datamanagers.Partner
import com.blockchain.swap.nabu.datamanagers.PartnerCredentials
import com.blockchain.swap.nabu.datamanagers.PaymentLimits
import com.blockchain.swap.nabu.datamanagers.PaymentMethod
import com.blockchain.swap.nabu.datamanagers.Product
import com.blockchain.swap.nabu.datamanagers.TransactionState
import com.blockchain.swap.nabu.datamanagers.TransactionType
import com.blockchain.swap.nabu.datamanagers.TransferDirection
import com.blockchain.swap.nabu.datamanagers.TransferLimits
import com.blockchain.swap.nabu.datamanagers.featureflags.BankLinkingEnabledProvider
import com.blockchain.swap.nabu.datamanagers.featureflags.Feature
import com.blockchain.swap.nabu.datamanagers.featureflags.FeatureEligibility
import com.blockchain.swap.nabu.datamanagers.repositories.AssetBalancesRepository
import com.blockchain.swap.nabu.datamanagers.repositories.interest.Eligibility
import com.blockchain.swap.nabu.datamanagers.repositories.interest.InterestLimits
import com.blockchain.swap.nabu.datamanagers.repositories.interest.InterestRepository
import com.blockchain.swap.nabu.datamanagers.repositories.swap.CustodialRepository
import com.blockchain.swap.nabu.datamanagers.repositories.swap.TradeTransactionItem
import com.blockchain.swap.nabu.extensions.fromIso8601ToUtc
import com.blockchain.swap.nabu.extensions.toLocalTime
import com.blockchain.swap.nabu.models.data.BankPartner
import com.blockchain.swap.nabu.models.data.LinkBankTransfer
import com.blockchain.swap.nabu.models.data.LinkedBank
import com.blockchain.swap.nabu.models.data.LinkedBankErrorState
import com.blockchain.swap.nabu.models.data.LinkedBankState
import com.blockchain.swap.nabu.models.responses.banktransfer.CreateLinkBankResponse
import com.blockchain.swap.nabu.models.responses.banktransfer.LinkedBankTransferResponse
import com.blockchain.swap.nabu.models.responses.banktransfer.ProviderAccountAttrs
import com.blockchain.swap.nabu.models.responses.banktransfer.UpdateProviderAccountBody
import com.blockchain.swap.nabu.models.responses.cards.CardResponse
import com.blockchain.swap.nabu.models.responses.cards.PaymentMethodResponse
import com.blockchain.swap.nabu.models.responses.interest.InterestAccountDetailsResponse
import com.blockchain.swap.nabu.models.responses.interest.InterestActivityItemResponse
import com.blockchain.swap.nabu.models.responses.interest.InterestRateResponse
import com.blockchain.swap.nabu.models.responses.nabu.AddAddressRequest
import com.blockchain.swap.nabu.models.responses.nabu.State
import com.blockchain.swap.nabu.models.responses.simplebuy.AddNewCardBodyRequest
import com.blockchain.swap.nabu.models.responses.simplebuy.AmountResponse
import com.blockchain.swap.nabu.models.responses.simplebuy.BankAccountResponse
import com.blockchain.swap.nabu.models.responses.simplebuy.BuyOrderListResponse
import com.blockchain.swap.nabu.models.responses.simplebuy.BuySellOrderResponse
import com.blockchain.swap.nabu.models.responses.simplebuy.CardPartnerAttributes
import com.blockchain.swap.nabu.models.responses.simplebuy.ConfirmOrderRequestBody
import com.blockchain.swap.nabu.models.responses.simplebuy.CustodialWalletOrder
import com.blockchain.swap.nabu.models.responses.simplebuy.TransactionResponse
import com.blockchain.swap.nabu.models.responses.simplebuy.TransferRequest
import com.blockchain.swap.nabu.models.responses.swap.CreateOrderRequest
import com.blockchain.swap.nabu.models.responses.swap.CustodialOrderResponse
import com.blockchain.swap.nabu.models.responses.tokenresponse.NabuSessionTokenResponse
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
    private val paymentAccountMapperMappers: Map<String, PaymentAccountMapper>,
    private val kycFeatureEligibility: FeatureEligibility,
    private val assetBalancesRepository: AssetBalancesRepository,
    private val interestRepository: InterestRepository,
    private val custodialRepository: CustodialRepository,
    private val extraAttributesProvider: ExtraAttributesProvider,
    private val bankLinkingEnabledProvider: BankLinkingEnabledProvider
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
                updateSupportedCards(it.methods)
            }
        }.ignoreElement()

    override fun getLinkedBeneficiaries(): Single<List<Beneficiary>> =
        authenticator.authenticate {
            nabuService.getBeneficiaries(it)
        }.map {
            it.map { beneficiary ->
                Beneficiary(
                    id = beneficiary.id,
                    title = "${beneficiary.name} ${beneficiary.agent.account}",
                    // address is returned from the api as ****6810
                    account = beneficiary.address.replace("*", ""),
                    currency = beneficiary.currency
                )
            }
        }

    override fun linkToABank(fiatCurrency: String): Single<LinkBankTransfer> {
        return authenticator.authenticate {
            nabuService.linkToABank(it, fiatCurrency, it.userId, extraAttributesProvider.getBankLinkingAttributes())
                .zipWith(bankLinkingEnabledProvider.supportedBankPartners())
        }.flatMap { (response, supportedPartners) ->
            response.partner.toLinkBankedPartner(supportedPartners)?.let {
                val attributes =
                    response.attributes
                        ?: return@flatMap Single.error<LinkBankTransfer>(IllegalStateException("Missing attributes"))
                Single.just(LinkBankTransfer(
                    response.id,
                    it,
                    it.attributes(attributes)
                ))
            }
        }
    }

    override fun updateAccountProviderId(linkingId: String, providerAccountId: String) =
        authenticator.authenticateCompletable {
            nabuService.updateAccountProviderId(it, linkingId, UpdateProviderAccountBody(
                ProviderAccountAttrs(providerAccountId, extraAttributesProvider.getBankUpdateOverride())
            ))
        }

    override fun fetchSuggestedPaymentMethod(
        fiatCurrency: String,
        onlyEligible: Boolean
    ): Single<List<PaymentMethod>> = paymentMethods(fiatCurrency, onlyEligible)

    private val updateSupportedCards: (List<PaymentMethodResponse>) -> Unit = {
        val cardTypes = it.filter { it.subTypes.isNullOrEmpty().not() }.mapNotNull {
            it.subTypes
        }.flatten().distinct()
        simpleBuyPrefs.updateSupportedCards(cardTypes.joinToString())
    }

    private fun getSupportedPaymentMethods(
        sessionTokenResponse: NabuSessionTokenResponse,
        fiatCurrency: String,
        onlyEligible: Boolean
    ) = nabuService.getPaymentMethodsForSimpleBuy(sessionTokenResponse, fiatCurrency).map { methods ->
        methods.filter { method -> method.eligible || !onlyEligible }
    }.doOnSuccess {
        updateSupportedCards(it)
    }.zipWith(bankLinkingEnabledProvider.bankLinkingEnabled()).map { (methods, enabled) ->
        methods.filter {
            it.type != PaymentMethodResponse.BANK_TRANSFER || enabled
        }
    }

    private fun paymentMethods(fiatCurrency: String, onlyEligible: Boolean) = authenticator.authenticate {
        Singles.zip(
            assetBalancesRepository.getTotalBalanceForAsset(fiatCurrency)
                .map { balance -> CustodialFiatBalance(fiatCurrency, true, balance) }
                .toSingle(CustodialFiatBalance(fiatCurrency, false, null)),
            nabuService.getCards(it).onErrorReturn { emptyList() },
            getLinkedBanks().onErrorReturn { emptyList() },
            getSupportedPaymentMethods(it, fiatCurrency, onlyEligible)
        ) { custodialFiatBalance, cardsResponse, linkedBanks, paymentMethods ->
            val availablePaymentMethods = mutableListOf<PaymentMethod>()

            paymentMethods.forEach { paymentMethod ->
                if (paymentMethod.type == PaymentMethodResponse.PAYMENT_CARD) {
                    val cardLimits = PaymentLimits(paymentMethod.limits.min, paymentMethod.limits.max, fiatCurrency)
                    cardsResponse.takeIf { cards -> cards.isNotEmpty() }?.filter { it.state.isActive() }
                        ?.forEach { cardResponse: CardResponse ->
                            availablePaymentMethods.add(cardResponse.toCardPaymentMethod(cardLimits))
                        }
                } else if (
                    paymentMethod.type == PaymentMethodResponse.FUNDS &&
                    paymentMethod.currency == fiatCurrency &&
                    SUPPORTED_FUNDS_CURRENCIES.contains(paymentMethod.currency)
                ) {
                    custodialFiatBalance.balance?.takeIf { balance ->
                        balance > FiatValue.fromMinor(
                            paymentMethod.currency,
                            paymentMethod.limits.min
                        )
                    }?.let { balance ->
                        val fundsLimits =
                            PaymentLimits(paymentMethod.limits.min,
                                paymentMethod.limits.max.coerceAtMost(balance.toBigInteger().toLong()),
                                paymentMethod.currency)
                        availablePaymentMethods.add(PaymentMethod.Funds(
                            balance,
                            paymentMethod.currency,
                            fundsLimits
                        ))
                    }
                    availablePaymentMethods.add(PaymentMethod.UndefinedFunds(
                        paymentMethod.currency,
                        PaymentLimits(paymentMethod.limits.min, paymentMethod.limits.max, paymentMethod.currency)))
                } else if (
                    paymentMethod.type == PaymentMethodResponse.BANK_TRANSFER &&
                    linkedBanks.isNotEmpty()
                ) {
                    val bankLimits = PaymentLimits(paymentMethod.limits.min, paymentMethod.limits.max, fiatCurrency)
                    linkedBanks.filter { linkedBank ->
                        linkedBank.state == LinkedBankState.ACTIVE
                    }.forEach { linkedBank: LinkedBank ->
                        availablePaymentMethods.add(linkedBank.toBankPaymentMethod(bankLimits))
                    }
                }
            }

            paymentMethods.firstOrNull { paymentMethod ->
                paymentMethod.type == PaymentMethodResponse.PAYMENT_CARD
            }?.let { paymentMethod ->
                availablePaymentMethods.add(
                    PaymentMethod.UndefinedCard(
                        PaymentLimits(
                            paymentMethod.limits.min,
                            paymentMethod.limits.max,
                            fiatCurrency
                        )
                    )
                )
            }

            paymentMethods.firstOrNull { paymentMethod ->
                paymentMethod.type == PaymentMethodResponse.BANK_TRANSFER
            }?.let { bankTransfer ->
                availablePaymentMethods.add(
                    PaymentMethod.UndefinedBankTransfer(PaymentLimits(bankTransfer.limits.min,
                        bankTransfer.limits.max,
                        fiatCurrency)))
            }

            if (!availablePaymentMethods.any { paymentMethod ->
                    paymentMethod is PaymentMethod.Card || paymentMethod is PaymentMethod.Funds
                }) {
                availablePaymentMethods.add(PaymentMethod.Undefined)
            }

            availablePaymentMethods.sortedBy { paymentMethod -> paymentMethod.order }.toList()
        }
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
        attributes: CardPartnerAttributes?,
        paymentMethodId: String?
    ): Single<BuySellOrder> =
        authenticator.authenticate {
            nabuService.confirmOrder(it, orderId,
                ConfirmOrderRequestBody(
                    paymentMethodId = paymentMethodId,
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

        return authenticator.authenticate {
            nabuService.getPaymentMethods(it, fiatCurrency, isTier2Approved)
        }.map { paymentMethodsResponse ->
            paymentMethodsResponse.methods.filter {
                it.type.toPaymentMethodType() == PaymentMethodType.FUNDS &&
                    SUPPORTED_FUNDS_CURRENCIES.contains(it.currency)
            }.mapNotNull {
                it.currency
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

    override fun createCustodialOrder(
        direction: TransferDirection,
        quoteId: String,
        volume: Money,
        destinationAddress: String?,
        refundAddress: String?
    ): Single<CustodialOrder> =
        authenticator.authenticate { sessionToken ->
            nabuService.createSwapOrder(
                sessionToken,
                CreateOrderRequest(
                    direction = direction.toString(),
                    quoteId = quoteId,
                    volume = volume.toBigInteger().toString(),
                    destinationAddress = destinationAddress,
                    refundAddress = refundAddress
                )
            ).map {
                it.toCustodialOrder() ?: throw IllegalStateException("Invalid order created")
            }
        }

    override fun getSwapLimits(currency: String): Single<TransferLimits> =
        authenticator.authenticate {
            nabuService.getSwapLimits(
                it,
                currency
            ).map { response ->
                if (response.maxOrder == null && response.minOrder == null && response.maxPossibleOrder == null) {
                    TransferLimits(currency)
                } else {
                    TransferLimits(
                        minLimit = FiatValue.fromMinor(currency, response.minOrder?.toLong() ?: 0L),
                        maxOrder = FiatValue.fromMinor(currency, response.maxOrder?.toLong() ?: 0L),
                        maxLimit = FiatValue.fromMinor(currency,
                            response.maxPossibleOrder?.toLong() ?: 0L)
                    )
                }
            }
        }

    override fun getCustodialActivityForAsset(
        cryptoCurrency: CryptoCurrency,
        directions: Set<TransferDirection>
    ): Single<List<TradeTransactionItem>> =
        custodialRepository.getCustodialActivityForAsset(cryptoCurrency, directions)

    override fun updateOrder(id: String, success: Boolean): Completable =
        authenticator.authenticateCompletable { sessionToken ->
            nabuService.updateOrder(
                sessionToken = sessionToken,
                id = id,
                success = success
            )
        }

    override fun getLinkedBank(id: String): Single<LinkedBank> =
        authenticator.authenticate { sessionToken ->
            nabuService.getLinkedBank(
                sessionToken = sessionToken,
                id = id
            )
        }.map {
            it.toLinkedBank()
        }

    override fun getLinkedBanks(): Single<List<LinkedBank>> {
        return authenticator.authenticate { sessionToken ->
            nabuService.getLinkedBanks(
                sessionToken = sessionToken
            )
        }.map { response ->
            response.mapNotNull {
                it.toLinkedBank()
            }
        }
    }

    private fun LinkedBankTransferResponse.toLinkedBank(): LinkedBank? {
        return LinkedBank(
            id = id,
            currency = currency,
            partner = partner.toLinkBankedPartner(BankPartner.values().toList()) ?: return null,
            state = state.toLinkedBankState(),
            name = details?.bankName ?: "",
            accountNumber = details?.accountNumber?.replace("x", "") ?: "",
            errorStatus = error?.toLinkedBankErrorState() ?: LinkedBankErrorState.NONE
        )
    }

    override fun isFiatCurrencySupported(destination: String): Boolean =
        SUPPORTED_FUNDS_CURRENCIES.contains(destination)

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

    private fun LinkedBank.toBankPaymentMethod(bankLimits: PaymentLimits) =
        PaymentMethod.Bank(
            bankId = this.id,
            limits = bankLimits,
            bankName = this.name,
            accountEnding = this.accountNumber
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

    private fun String.toLinkedBankState(): LinkedBankState =
        when (this) {
            LinkedBankTransferResponse.ACTIVE -> LinkedBankState.ACTIVE
            LinkedBankTransferResponse.PENDING -> LinkedBankState.PENDING
            LinkedBankTransferResponse.BLOCKED -> LinkedBankState.BLOCKED
            else -> LinkedBankState.UNKNOWN
        }

    override fun getSwapTrades(): Single<List<CustodialOrder>> =
        authenticator.authenticate { sessionToken ->
            nabuService.getSwapTrades(sessionToken)
        }.map { response ->
            response.mapNotNull { orderResp ->
                orderResp.toSwapOrder()
            }
        }

    private fun CustodialOrderResponse.toSwapOrder(): CustodialOrder? {
        return CustodialOrder(
            id = this.id,
            state = this.state.toCustodialOrderState(),
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

    private fun CustodialOrderResponse.toCustodialOrder(): CustodialOrder? {
        return CustodialOrder(
            id = this.id,
            state = this.state.toCustodialOrderState(),
            depositAddress = this.kind.depositAddress,
            createdAt = this.createdAt.fromIso8601ToUtc() ?: Date(),
            inputMoney = CurrencyPair.fromRawPair(pair, SUPPORTED_FUNDS_CURRENCIES)?.let {
                it.toSourceMoney(priceFunnel.inputMoney.toBigInteger())
            } ?: return null,
            outputMoney = CurrencyPair.fromRawPair(pair, SUPPORTED_FUNDS_CURRENCIES)?.let {
                it.toDestinationMoney(priceFunnel.outputMoney.toBigInteger())
            } ?: return null
        )
    }

    companion object {
        internal val SUPPORTED_FUNDS_CURRENCIES = listOf(
            "GBP", "EUR", "USD"
        )
    }
}

private fun String.toLinkedBankState(): LinkedBankState =
    when (this) {
        LinkedBankTransferResponse.ACTIVE -> LinkedBankState.ACTIVE
        LinkedBankTransferResponse.PENDING -> LinkedBankState.PENDING
        LinkedBankTransferResponse.BLOCKED -> LinkedBankState.BLOCKED
        else -> LinkedBankState.UNKNOWN
    }

private fun String.toLinkBankedPartner(supportedBankPartners: List<BankPartner>): BankPartner? {
    val partner = when (this) {
        CreateLinkBankResponse.YODLEE_PARTNER -> BankPartner.YODLEE
        else -> null
    }

    return if (supportedBankPartners.contains(partner)) {
        partner
    } else null
}

private fun String.toLinkedBankErrorState(): LinkedBankErrorState =
    when (this) {
        LinkedBankTransferResponse.ERROR_ALREADY_LINKED -> LinkedBankErrorState.ACCOUNT_ALREADY_LINKED
        LinkedBankTransferResponse.ERROR_UNSUPPORTED_ACCOUNT -> LinkedBankErrorState.ACCOUNT_TYPE_UNSUPPORTED
        else -> LinkedBankErrorState.UNKNOWN
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

fun String.toCustodialOrderState(): CustodialOrderState =
    when (this) {
        CustodialOrderResponse.CREATED -> CustodialOrderState.CREATED
        CustodialOrderResponse.PENDING_CONFIRMATION -> CustodialOrderState.PENDING_CONFIRMATION
        CustodialOrderResponse.PENDING_EXECUTION -> CustodialOrderState.PENDING_EXECUTION
        CustodialOrderResponse.PENDING_DEPOSIT -> CustodialOrderState.PENDING_DEPOSIT
        CustodialOrderResponse.PENDING_LEDGER -> CustodialOrderState.PENDING_LEDGER
        CustodialOrderResponse.FINISH_DEPOSIT -> CustodialOrderState.FINISH_DEPOSIT
        CustodialOrderResponse.PENDING_WITHDRAWAL -> CustodialOrderState.PENDING_WITHDRAWAL
        CustodialOrderResponse.EXPIRED -> CustodialOrderState.EXPIRED
        CustodialOrderResponse.FINISHED -> CustodialOrderState.FINISHED
        CustodialOrderResponse.CANCELED -> CustodialOrderState.CANCELED
        CustodialOrderResponse.FAILED -> CustodialOrderState.FAILED
        else -> CustodialOrderState.UNKNOWN
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
    BANK_TRANSFER,
    PAYMENT_CARD,
    FUNDS,
    UNKNOWN;

    fun toAnalyticsString() =
        when (this) {
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
                PaymentMethodType.FUNDS -> PaymentMethod.FUNDS_PAYMENT_ID
                PaymentMethodType.BANK_TRANSFER -> PaymentMethod.UNDEFINED_BANK_TRANSFER_PAYMENT_ID
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
        type = type(),
        depositPaymentId = depositPaymentId ?: ""
    )
}

private fun String.toPaymentMethodType(): PaymentMethodType =
    when (this) {
        PaymentMethodResponse.PAYMENT_CARD -> PaymentMethodType.PAYMENT_CARD
        PaymentMethodResponse.BANK_TRANSFER -> PaymentMethodType.BANK_TRANSFER
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