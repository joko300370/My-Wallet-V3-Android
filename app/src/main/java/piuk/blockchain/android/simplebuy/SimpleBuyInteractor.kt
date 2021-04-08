package piuk.blockchain.android.simplebuy

import com.blockchain.nabu.datamanagers.BillingAddress
import com.blockchain.nabu.datamanagers.BuySellOrder
import com.blockchain.nabu.datamanagers.BuySellPairs
import com.blockchain.nabu.datamanagers.CardToBeActivated
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.EligibilityProvider
import com.blockchain.nabu.datamanagers.EligiblePaymentMethodType
import com.blockchain.nabu.datamanagers.OrderInput
import com.blockchain.nabu.datamanagers.OrderOutput
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.TransferLimits
import com.blockchain.nabu.datamanagers.custodialwalletimpl.CardStatus
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.datamanagers.repositories.WithdrawLocksRepository
import com.blockchain.nabu.models.data.BankPartner
import com.blockchain.nabu.models.data.LinkedBank
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.models.responses.simplebuy.CustodialWalletOrder
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyConfirmationAttributes
import com.blockchain.nabu.service.TierService
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.BankLinkingPrefs
import com.blockchain.ui.trackProgress
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import piuk.blockchain.android.cards.CardIntent
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.networking.PollService
import piuk.blockchain.android.sdd.SDDAnalytics
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.linkbank.BankLinkingInfo
import piuk.blockchain.android.util.AppUtil
import java.util.concurrent.TimeUnit

class SimpleBuyInteractor(
    private val tierService: TierService,
    private val custodialWalletManager: CustodialWalletManager,
    private val withdrawLocksRepository: WithdrawLocksRepository,
    private val appUtil: AppUtil,
    private val analytics: Analytics,
    private val eligibilityProvider: EligibilityProvider,
    private val coincore: Coincore,
    private val bankLinkingPrefs: BankLinkingPrefs
) {

    // ignore limits when user is in tier 0
    fun fetchBuyLimitsAndSupportedCryptoCurrencies(
        targetCurrency: String
    ): Single<Pair<BuySellPairs, TransferLimits?>> =
        custodialWalletManager.getSupportedBuySellCryptoCurrencies(targetCurrency).zipWith(
            tierService.tiers()
        ).flatMap { (pairs, tier) ->
            if (tier.isInInitialState()) {
                Single.just(pairs to null)
            } else
                custodialWalletManager.getProductTransferLimits(targetCurrency, product = Product.SIMPLEBUY).map {
                    pairs to it
                }
        }.trackProgress(appUtil.activityIndicator)

    fun fetchSupportedFiatCurrencies(): Single<SimpleBuyIntent.SupportedCurrenciesUpdated> =
        custodialWalletManager.getSupportedFiatCurrencies()
            .map { SimpleBuyIntent.SupportedCurrenciesUpdated(it) }
            .trackProgress(appUtil.activityIndicator)

    fun cancelOrder(orderId: String): Completable =
        custodialWalletManager.deleteBuyOrder(orderId)

    fun createOrder(
        cryptoCurrency: CryptoCurrency,
        amount: FiatValue,
        paymentMethodId: String? = null,
        paymentMethod: PaymentMethodType,
        isPending: Boolean
    ): Single<SimpleBuyIntent.OrderCreated> =
        custodialWalletManager.createOrder(
            custodialWalletOrder = CustodialWalletOrder(
                pair = "${cryptoCurrency.networkTicker}-${amount.currencyCode}",
                action = "BUY",
                input = OrderInput(
                    amount.currencyCode, amount.toBigInteger().toString()
                ),
                output = OrderOutput(
                    cryptoCurrency.networkTicker, null
                ),
                paymentMethodId = paymentMethodId,
                paymentType = paymentMethod.name
            ),
            stateAction = if (isPending) "pending" else null
        ).map {
            SimpleBuyIntent.OrderCreated(it)
        }

    fun fetchWithdrawLockTime(paymentMethod: PaymentMethodType): Single<SimpleBuyIntent.WithdrawLocksTimeUpdated> =
        withdrawLocksRepository.getWithdrawLockTypeForPaymentMethod(paymentMethod)
            .map {
                SimpleBuyIntent.WithdrawLocksTimeUpdated(it)
            }.onErrorReturn {
                SimpleBuyIntent.WithdrawLocksTimeUpdated()
            }

    fun fetchQuote(cryptoCurrency: CryptoCurrency?, amount: FiatValue?): Single<SimpleBuyIntent.QuoteUpdated> =
        custodialWalletManager.getQuote(
            cryptoCurrency = cryptoCurrency ?: throw IllegalStateException("Missing Cryptocurrency "),
            fiatCurrency = amount?.currencyCode ?: throw IllegalStateException("Missing FiatCurrency "),
            action = "BUY",
            currency = amount.currencyCode,
            amount = amount.toBigInteger().toString()
        ).map {
            SimpleBuyIntent.QuoteUpdated(it)
        }

    fun pollForKycState(): Single<SimpleBuyIntent.KycStateUpdated> =
        tierService.tiers()
            .flatMap {
                when {
                    it.isApprovedFor(KycTierLevel.GOLD) ->
                        eligibilityProvider.isEligibleForSimpleBuy(forceRefresh = true).map { eligible ->
                            if (eligible) {
                                SimpleBuyIntent.KycStateUpdated(KycState.VERIFIED_AND_ELIGIBLE)
                            } else {
                                SimpleBuyIntent.KycStateUpdated(KycState.VERIFIED_BUT_NOT_ELIGIBLE)
                            }
                        }
                    it.isRejectedForAny() -> Single.just(SimpleBuyIntent.KycStateUpdated(KycState.FAILED))
                    it.isInReviewForAny() -> Single.just(SimpleBuyIntent.KycStateUpdated(KycState.IN_REVIEW))
                    else -> Single.just(SimpleBuyIntent.KycStateUpdated(KycState.PENDING))
                }
            }.onErrorReturn {
                SimpleBuyIntent.KycStateUpdated(KycState.PENDING)
            }
            .repeatWhen { it.delay(INTERVAL, TimeUnit.SECONDS).zipWith(Flowable.range(0, RETRIES_SHORT)) }
            .takeUntil { it.kycState != KycState.PENDING }
            .last(SimpleBuyIntent.KycStateUpdated(KycState.PENDING))
            .map {
                if (it.kycState == KycState.PENDING) {
                    SimpleBuyIntent.KycStateUpdated(KycState.UNDECIDED)
                } else {
                    it
                }
            }

    fun updateSelectedBankAccountId(
        linkingId: String,
        providerAccountId: String = "",
        accountId: String,
        partner: BankPartner,
        source: BankAuthSource
    ): Completable {
        val linkingInfo = BankLinkingInfo(linkingId, source)
        bankLinkingPrefs.setBankLinkingInfo(BankLinkingInfo.toJson(linkingInfo))

        return custodialWalletManager.updateSelectedBankAccount(
            linkingId = linkingId,
            providerAccountId = providerAccountId,
            accountId = accountId,
            partner = partner
        )
    }

    fun pollForLinkedBankState(id: String, partner: BankPartner?): Single<LinkedBank> = PollService(
        custodialWalletManager.getLinkedBank(id)
    ) {
        !it.isLinkingPending() || (it.partner == partner && it.isLinkingPending())
    }.start(timerInSec = INTERVAL, retries = RETRIES_DEFAULT).map {
        it.value
    }

    fun pollForBankLinkingCompleted(id: String): Single<LinkedBank> = PollService(
        custodialWalletManager.getLinkedBank(id)
    ) {
        it.isLinkingInFinishedState()
    }.start(timerInSec = INTERVAL, retries = RETRIES_DEFAULT).map {
        it.value
    }

    fun checkTierLevel(): Single<SimpleBuyIntent.KycStateUpdated> {

        return tierService.tiers().flatMap {
            when {
                it.isApprovedFor(KycTierLevel.GOLD) -> eligibilityProvider.isEligibleForSimpleBuy(forceRefresh = true)
                    .map { eligible ->
                        if (eligible) {
                            SimpleBuyIntent.KycStateUpdated(KycState.VERIFIED_AND_ELIGIBLE)
                        } else {
                            SimpleBuyIntent.KycStateUpdated(KycState.VERIFIED_BUT_NOT_ELIGIBLE)
                        }
                    }
                it.isRejectedFor(KycTierLevel.GOLD) -> Single.just(SimpleBuyIntent.KycStateUpdated(KycState.FAILED))
                it.isPendingFor(KycTierLevel.GOLD) -> Single.just(SimpleBuyIntent.KycStateUpdated(KycState.IN_REVIEW))
                else -> Single.just(SimpleBuyIntent.KycStateUpdated(KycState.PENDING))
            }
        }.onErrorReturn { SimpleBuyIntent.KycStateUpdated(KycState.PENDING) }
    }

    fun linkNewBank(fiatCurrency: String): Single<SimpleBuyIntent.BankLinkProcessStarted> {
        return custodialWalletManager.linkToABank(fiatCurrency).map {
            SimpleBuyIntent.BankLinkProcessStarted(
                it
            )
        }.trackProgress(appUtil.activityIndicator)
    }

    private fun KycTiers.isRejectedForAny(): Boolean =
        isRejectedFor(KycTierLevel.SILVER) ||
            isRejectedFor(KycTierLevel.GOLD)

    private fun KycTiers.isInReviewForAny(): Boolean =
        isUnderReviewFor(KycTierLevel.SILVER) ||
            isUnderReviewFor(KycTierLevel.GOLD)

    fun exchangeRate(cryptoCurrency: CryptoCurrency): Single<SimpleBuyIntent.ExchangeRateUpdated> =
        coincore[cryptoCurrency].exchangeRate().map {
            SimpleBuyIntent.ExchangeRateUpdated(it.price() as FiatValue)
        }

    fun eligiblePaymentMethods(fiatCurrency: String, preselectedId: String?):
        Single<SimpleBuyIntent.PaymentMethodsUpdated> =
        tierService.tiers().zipWith(custodialWalletManager.isSimplifiedDueDiligenceEligible().onErrorReturn { false }
            .doOnSuccess {
                if (it) {
                    analytics.logEventOnce(SDDAnalytics.SDD_ELIGIBLE)
                }
            })
            .flatMap { (tier, sddEligible) ->
                custodialWalletManager.fetchSuggestedPaymentMethod(
                    fiatCurrency = fiatCurrency,
                    fetchSddLimits = sddEligible && tier.isInInitialState(),
                    onlyEligible = tier.isInitialisedFor(KycTierLevel.GOLD)
                ).map { paymentMethods ->
                    SimpleBuyIntent.PaymentMethodsUpdated(
                        availablePaymentMethods = paymentMethods,
                        canLinkBank = paymentMethods.filterIsInstance<PaymentMethod.UndefinedBankTransfer>()
                            .firstOrNull()?.isEligible ?: false,
                        canAddCard = paymentMethods.filterIsInstance<PaymentMethod.UndefinedCard>()
                            .firstOrNull()?.isEligible ?: false,
                        canLinkFunds = paymentMethods.filterIsInstance<PaymentMethod.UndefinedFunds>()
                            .firstOrNull()?.isEligible ?: false,
                        preselectedId = preselectedId
                    )
                }
            }

    // attributes are null in case of bank
    fun confirmOrder(
        orderId: String,
        paymentMethodId: String?,
        attributes: SimpleBuyConfirmationAttributes?,
        isBankPartner: Boolean?
    ): Single<BuySellOrder> = custodialWalletManager.confirmOrder(orderId, attributes, paymentMethodId, isBankPartner)

    fun pollForOrderStatus(orderId: String): Single<BuySellOrder> =
        custodialWalletManager.getBuyOrder(orderId)
            .repeatWhen { it.delay(INTERVAL, TimeUnit.SECONDS).zipWith(Flowable.range(0, RETRIES_LONG)) }
            .takeUntil {
                it.state == OrderState.FINISHED ||
                    it.state == OrderState.FAILED ||
                    it.state == OrderState.CANCELED
            }.lastOrError()

    fun pollForAuthorisationUrl(orderId: String): Single<BuySellOrder> =
        PollService(
            custodialWalletManager.getBuyOrder(orderId)
        ) {
            it.attributes?.authorisationUrl != null
        }.start()
            .map {
                it.value
            }

    fun pollForCardStatus(cardId: String): Single<CardIntent.CardUpdated> =
        PollService(
            custodialWalletManager.getCardDetails(cardId)
        ) {
            it.status == CardStatus.BLOCKED ||
                it.status == CardStatus.EXPIRED ||
                it.status == CardStatus.ACTIVE
        }
            .start()
            .map {
                CardIntent.CardUpdated(it.value)
            }

    fun eligiblePaymentMethodsTypes(fiatCurrency: String): Single<List<EligiblePaymentMethodType>> =
        custodialWalletManager.getEligiblePaymentMethodTypes(
            fiatCurrency = fiatCurrency
        )

    fun getLinkedBankInfo(paymentMethodId: String) =
        custodialWalletManager.getLinkedBank(paymentMethodId)

    fun fetchOrder(orderId: String) = custodialWalletManager.getBuyOrder(orderId)

    fun addNewCard(fiatCurrency: String, billingAddress: BillingAddress): Single<CardToBeActivated> =
        custodialWalletManager.addNewCard(fiatCurrency, billingAddress)

    fun userIsEligibleToLinkABank(fiatCurrency: String): Single<Boolean> =
        custodialWalletManager.getEligiblePaymentMethodTypes(fiatCurrency).map {
            it.contains(EligiblePaymentMethodType(PaymentMethodType.BANK_TRANSFER, fiatCurrency))
        }

    fun updateApprovalStatus() = bankLinkingPrefs.setPaymentApprovalConsumed(false)

    companion object {
        private const val INTERVAL: Long = 5
        private const val RETRIES_SHORT = 6
        private const val RETRIES_DEFAULT = 12
        private const val RETRIES_LONG = 20
    }
}
