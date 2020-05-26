package piuk.blockchain.android.simplebuy

import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.datamanagers.BillingAddress
import com.blockchain.swap.nabu.datamanagers.BuyOrder
import com.blockchain.swap.nabu.datamanagers.CardToBeActivated
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.swap.nabu.datamanagers.SimpleBuyPairs
import com.blockchain.swap.nabu.datamanagers.custodialwalletimpl.CardStatus
import com.blockchain.swap.nabu.models.nabu.Kyc2TierState
import com.blockchain.swap.nabu.models.simplebuy.CardPartnerAttributes
import com.blockchain.swap.nabu.service.TierService
import com.blockchain.ui.trackLoading
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import piuk.blockchain.android.cards.CardIntent
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.util.AppUtil
import java.lang.IllegalStateException
import java.util.concurrent.TimeUnit

class SimpleBuyInteractor(
    private val tierService: TierService,
    private val custodialWalletManager: CustodialWalletManager,
    private val nabu: NabuToken,
    private val appUtil: AppUtil,
    private val coincore: Coincore
) {

    fun fetchBuyLimitsAndSupportedCryptoCurrencies(targetCurrency: String):
            Single<SimpleBuyPairs> =
        nabu.fetchNabuToken()
            .flatMap { custodialWalletManager.getBuyLimitsAndSupportedCryptoCurrencies(it, targetCurrency) }
            .trackLoading(appUtil.activityIndicator)

    fun fetchSupportedFiatCurrencies(): Single<SimpleBuyIntent.SupportedCurrenciesUpdated> =
        nabu.fetchNabuToken()
            .flatMap { custodialWalletManager.getSupportedFiatCurrencies(it) }
            .map { SimpleBuyIntent.SupportedCurrenciesUpdated(it) }
            .trackLoading(appUtil.activityIndicator)

    fun fetchPredefinedAmounts(targetCurrency: String): Single<SimpleBuyIntent.UpdatedPredefinedAmounts> =
        custodialWalletManager.getPredefinedAmounts(targetCurrency)
            .map {
                SimpleBuyIntent.UpdatedPredefinedAmounts(it.sortedBy { value ->
                    value.valueMinor
                })
            }.trackLoading(appUtil.activityIndicator)

    fun cancelOrder(orderId: String): Completable =
        custodialWalletManager.deleteBuyOrder(orderId)

    fun createOrder(
        cryptoCurrency: CryptoCurrency,
        amount: FiatValue,
        paymentMethodId: String? = null,
        isPending: Boolean
    ): Single<SimpleBuyIntent.OrderCreated> =
        custodialWalletManager.createOrder(
            cryptoCurrency = cryptoCurrency,
            action = "BUY",
            amount = amount,
            paymentMethodId = paymentMethodId,
            stateAction = if (isPending) "pending" else null
        ).map {
            SimpleBuyIntent.OrderCreated(it)
        }

    fun fetchBankAccount(currency: String): Single<SimpleBuyIntent.BankAccountUpdated> =
        custodialWalletManager.getBankAccountDetails(currency).map {
            SimpleBuyIntent.BankAccountUpdated(it)
        }

    fun fetchQuote(cryptoCurrency: CryptoCurrency?, amount: FiatValue?): Single<SimpleBuyIntent.QuoteUpdated> =
        custodialWalletManager.getQuote(
            crypto = cryptoCurrency ?: throw IllegalStateException("Missing Cryptocurrency "),
            action = "BUY",
            amount = amount ?: throw IllegalStateException("Missing amount ")).map {
            SimpleBuyIntent.QuoteUpdated(it)
        }

    fun pollForKycState(fiatCurrency: String): Single<SimpleBuyIntent.KycStateUpdated> =
        tierService.tiers()
            .flatMap {
                when {
                    it.combinedState == Kyc2TierState.Tier2Approved ->
                        custodialWalletManager.isEligibleForSimpleBuy(fiatCurrency).map { eligible ->
                            if (eligible) {
                                SimpleBuyIntent.KycStateUpdated(KycState.VERIFIED_AND_ELIGIBLE)
                            } else {
                                SimpleBuyIntent.KycStateUpdated(KycState.VERIFIED_BUT_NOT_ELIGIBLE)
                            }
                        }
                    it.combinedState.isRejected() ->
                        Single.just(SimpleBuyIntent.KycStateUpdated(KycState.FAILED))
                    it.combinedState.isInReview() ->
                        Single.just(SimpleBuyIntent.KycStateUpdated(KycState.IN_REVIEW))
                    else -> Single.just(SimpleBuyIntent.KycStateUpdated(KycState.PENDING))
                }
            }.onErrorReturn {
                SimpleBuyIntent.KycStateUpdated(KycState.PENDING)
            }
            .repeatWhen { it.delay(5, TimeUnit.SECONDS).zipWith(Flowable.range(0, 6)) }
            .takeUntil { it.kycState != KycState.PENDING }
            .last(SimpleBuyIntent.KycStateUpdated(KycState.PENDING))
            .map {
                if (it.kycState == KycState.PENDING) {
                    return@map SimpleBuyIntent.KycStateUpdated(KycState.UNDECIDED)
                } else {
                    return@map it
                }
            }

    fun checkTierLevel(fiatCurrency: String): Single<SimpleBuyIntent.KycStateUpdated> {

        return tierService.tiers().flatMap {
            when (it.combinedState) {
                Kyc2TierState.Tier2Approved -> custodialWalletManager.isEligibleForSimpleBuy(fiatCurrency)
                    .map { eligible ->
                        if (eligible) {
                            SimpleBuyIntent.KycStateUpdated(KycState.VERIFIED_AND_ELIGIBLE)
                        } else {
                            SimpleBuyIntent.KycStateUpdated(KycState.VERIFIED_BUT_NOT_ELIGIBLE)
                        }
                    }
                Kyc2TierState.Tier2Failed -> Single.just(SimpleBuyIntent.KycStateUpdated(KycState.FAILED))
                Kyc2TierState.Tier2InPending -> Single.just(SimpleBuyIntent.KycStateUpdated(KycState.IN_REVIEW))
                else -> Single.just(SimpleBuyIntent.KycStateUpdated(KycState.PENDING))
            }
        }.onErrorReturn { SimpleBuyIntent.KycStateUpdated(KycState.PENDING) }
    }

    private fun Kyc2TierState.isRejected(): Boolean =
        this == Kyc2TierState.Tier1Failed ||
                this == Kyc2TierState.Tier2Failed

    private fun Kyc2TierState.isInReview(): Boolean =
        this == Kyc2TierState.Tier1InReview ||
                this == Kyc2TierState.Tier2InReview

    fun exchangeRate(cryptoCurrency: CryptoCurrency): Single<SimpleBuyIntent.ExchangeRateUpdated> =
        coincore[cryptoCurrency].exchangeRate().map {
            SimpleBuyIntent.ExchangeRateUpdated(it)
        }

    fun fetchPaymentMethods(fiatCurrency: String, preselectedId: String?):
            Single<SimpleBuyIntent.PaymentMethodsUpdated> =
        tierService.tiers().flatMap { tier ->
            custodialWalletManager.fetchSuggestedPaymentMethod(fiatCurrency,
                tier.combinedState == Kyc2TierState.Tier2Approved
            ).map {
                SimpleBuyIntent.PaymentMethodsUpdated(
                    it,
                    tier.combinedState == Kyc2TierState.Tier2Approved,
                    preselectedId
                )
            }
        }

    // attributes are null in case of bank
    fun confirmOrder(orderId: String, attributes: CardPartnerAttributes?): Single<BuyOrder> =
        custodialWalletManager.confirmOrder(orderId, attributes)

    fun pollForOrderStatus(orderId: String): Single<BuyOrder> =
        custodialWalletManager.getBuyOrder(orderId)
            .repeatWhen { it.delay(5, TimeUnit.SECONDS).zipWith(Flowable.range(0, 20)) }
            .takeUntil {
                it.state == OrderState.FINISHED ||
                        it.state == OrderState.FAILED ||
                        it.state == OrderState.CANCELED
            }.lastOrError()

    fun pollForCardStatus(cardId: String): Single<CardIntent.CardUpdated> =
        custodialWalletManager.getCardDetails(cardId)
            .repeatWhen { it.delay(5, TimeUnit.SECONDS).zipWith(Flowable.range(0, 20)) }
            .takeUntil {
                it.status == CardStatus.BLOCKED ||
                        it.status == CardStatus.EXPIRED ||
                        it.status == CardStatus.ACTIVE
            }
            .map {
                CardIntent.CardUpdated(it)
            }
            .lastOrError()

    fun fetchOrder(orderId: String) = custodialWalletManager.getBuyOrder(orderId)

    fun addNewCard(fiatCurrency: String, billingAddress: BillingAddress): Single<CardToBeActivated> =
        custodialWalletManager.addNewCard(fiatCurrency, billingAddress)
}