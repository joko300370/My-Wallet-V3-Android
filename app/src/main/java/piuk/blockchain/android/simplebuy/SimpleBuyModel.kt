package piuk.blockchain.android.simplebuy

import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.datamanagers.BuySellOrder
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.UndefinedPaymentMethod
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.LinkedBankErrorState
import com.blockchain.nabu.models.data.LinkedBankState
import com.blockchain.nabu.models.responses.nabu.NabuApiException
import com.blockchain.nabu.models.responses.nabu.NabuErrorCodes
import com.blockchain.nabu.models.responses.simplebuy.EverypayPaymentAttrs
import com.blockchain.preferences.RatingPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.cards.partners.CardActivator
import piuk.blockchain.android.cards.partners.EverypayCardActivator
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.utils.extensions.thenSingle

class SimpleBuyModel(
    private val prefs: SimpleBuyPrefs,
    private val ratingPrefs: RatingPrefs,
    initialState: SimpleBuyState,
    scheduler: Scheduler,
    private val gson: Gson,
    private val cardActivators: List<CardActivator>,
    private val interactor: SimpleBuyInteractor,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<SimpleBuyState, SimpleBuyIntent>(
    gson.fromJson(prefs.simpleBuyState(), SimpleBuyState::class.java) ?: initialState,
    scheduler,
    environmentConfig,
    crashLogger
) {

    override fun performAction(previousState: SimpleBuyState, intent: SimpleBuyIntent): Disposable? =
        when (intent) {
            is SimpleBuyIntent.FetchBuyLimits ->
                interactor.fetchBuyLimitsAndSupportedCryptoCurrencies(intent.fiatCurrency)
                    .subscribeBy(
                        onSuccess = { (pairs, transferLimits) ->
                            process(
                                SimpleBuyIntent.UpdatedBuyLimitsAndSupportedCryptoCurrencies(
                                    pairs,
                                    intent.cryptoCurrency,
                                    transferLimits
                                )
                            )
                            process(SimpleBuyIntent.NewCryptoCurrencySelected(intent.cryptoCurrency))
                        },
                        onError = { process(SimpleBuyIntent.ErrorIntent()) }
                    )
            is SimpleBuyIntent.FetchSupportedFiatCurrencies ->
                interactor.fetchSupportedFiatCurrencies()
                    .subscribeBy(
                        onSuccess = { process(it) },
                        onError = { process(SimpleBuyIntent.ErrorIntent()) }
                    )
            is SimpleBuyIntent.CancelOrder -> (previousState.id?.let {
                interactor.cancelOrder(it)
            } ?: Completable.complete())
                .subscribeBy(
                    onComplete = { process(SimpleBuyIntent.OrderCanceled) },
                    onError = { process(SimpleBuyIntent.ErrorIntent()) }
                )
            is SimpleBuyIntent.CancelOrderIfAnyAndCreatePendingOne -> (previousState.id?.let {
                interactor.cancelOrder(it)
            } ?: Completable.complete()).thenSingle {
                interactor.createOrder(
                    previousState.selectedCryptoCurrency
                        ?: throw IllegalStateException("Missing Cryptocurrency "),
                    previousState.order.amount ?: throw IllegalStateException("Missing amount"),
                    previousState.selectedPaymentMethod?.concreteId(),
                    previousState.selectedPaymentMethod?.paymentMethodType
                        ?: throw IllegalStateException("Missing Payment Method"),
                    true
                )
            }.subscribeBy(
                onSuccess = {
                    process(it)
                },
                onError = {
                    process(SimpleBuyIntent.ErrorIntent())
                }
            )

            is SimpleBuyIntent.FetchKycState -> interactor.pollForKycState()
                .subscribeBy(
                    onSuccess = { process(it) },
                    onError = { /*never fails. will return SimpleBuyIntent.KycStateUpdated(KycState.FAILED)*/ }
                )

            is SimpleBuyIntent.FetchQuote -> interactor.fetchQuote(
                previousState.selectedCryptoCurrency,
                previousState.order.amount
            ).subscribeBy(
                onSuccess = { process(it) },
                onError = { process(SimpleBuyIntent.ErrorIntent()) }
            )

            is SimpleBuyIntent.LinkBankTransferRequested -> interactor.linkNewBank(previousState.fiatCurrency)
                .subscribeBy(
                    onSuccess = { process(it) },
                    onError = { process(SimpleBuyIntent.ErrorIntent(ErrorState.LinkedBankNotSupported)) }
                )
            is SimpleBuyIntent.TryToLinkABankTransfer -> {
                interactor.eligiblePaymentMethodsTypes(previousState.fiatCurrency).map {
                    it.any { paymentMethod -> paymentMethod.paymentMethodType == PaymentMethodType.BANK_TRANSFER }
                }.subscribeBy(
                    onSuccess = { isEligibleToLinkABank ->
                        if (isEligibleToLinkABank) {
                            process(SimpleBuyIntent.LinkBankTransferRequested)
                        } else {
                            process(SimpleBuyIntent.ErrorIntent(ErrorState.LinkedBankNotSupported))
                        }
                    },
                    onError = {
                        process(SimpleBuyIntent.ErrorIntent(ErrorState.LinkedBankNotSupported))
                    }
                )
            }

            is SimpleBuyIntent.UpdateAccountProvider -> {
                interactor.updateAccountProviderId(
                    linkingId = intent.linkingBankId,
                    providerAccountId = intent.accountProviderId,
                    accountId = intent.accountId
                ).subscribeBy(
                    onComplete = {
                        process(SimpleBuyIntent.StartPollingForLinkStatus(intent.linkingBankId))
                    },
                    onError = {
                        process(SimpleBuyIntent.ProviderAccountIdUpdateError)
                    }
                )
            }

            is SimpleBuyIntent.StartPollingForLinkStatus -> {
                interactor.pollForLinkedBankState(intent.bankId).subscribeBy(
                    onSuccess = {
                        when (it.state) {
                            LinkedBankState.ACTIVE -> {
                                process(SimpleBuyIntent.LinkedBankStateSuccess(it))
                            }
                            LinkedBankState.BLOCKED,
                            LinkedBankState.UNKNOWN -> {
                                when (it.errorStatus) {
                                    LinkedBankErrorState.ACCOUNT_ALREADY_LINKED -> {
                                        process(SimpleBuyIntent.LinkedBankStateAlreadyLinked)
                                    }
                                    LinkedBankErrorState.UNKNOWN -> {
                                        process(SimpleBuyIntent.LinkedBankStateError)
                                    }
                                    LinkedBankErrorState.ACCOUNT_TYPE_UNSUPPORTED -> {
                                        process(SimpleBuyIntent.LinkedBankStateUnsupportedAccount)
                                    }
                                    LinkedBankErrorState.NAMES_MISS_MATCHED -> {
                                        process(SimpleBuyIntent.LinkedBankStateNamesMissMatch)
                                    }
                                    LinkedBankErrorState.NONE -> {
                                        // do nothing
                                    }
                                }
                            }
                            LinkedBankState.PENDING -> {
                                process(SimpleBuyIntent.LinkedBankStateTimeout)
                            }
                        }
                    },
                    onError = {
                        process(SimpleBuyIntent.LinkedBankStateError)
                    }
                )
            }

            is SimpleBuyIntent.UpdateExchangeRate -> interactor.exchangeRate(intent.currency)
                .subscribeBy(
                    onSuccess = { process(it) },
                    onError = { }
                )
            is SimpleBuyIntent.NewCryptoCurrencySelected -> interactor.exchangeRate(intent.currency)
                .subscribeBy(
                    onSuccess = { process(it) },
                    onError = { }
                )
            is SimpleBuyIntent.FetchWithdrawLockTime -> {
                require(previousState.selectedPaymentMethod != null)
                interactor.fetchWithdrawLockTime(
                    previousState.selectedPaymentMethod.paymentMethodType
                )
                    .subscribeBy(
                        onSuccess = { process(it) },
                        onError = { }
                    )
            }
            is SimpleBuyIntent.BuyButtonClicked -> interactor.checkTierLevel()
                .subscribeBy(
                    onSuccess = { process(it) },
                    onError = { process(SimpleBuyIntent.ErrorIntent()) }
                )

            is SimpleBuyIntent.FetchSuggestedPaymentMethod -> interactor.eligiblePaymentMethods(
                intent.fiatCurrency,
                intent.selectedPaymentMethodId
            )
                .subscribeBy(
                    onSuccess = {
                        process(it)
                    },
                    onError = {
                        process(SimpleBuyIntent.ErrorIntent())
                    }
                )
            is SimpleBuyIntent.PaymentMethodChangeRequested -> {
                if (intent.paymentMethod.isEligible && intent.paymentMethod is UndefinedPaymentMethod) {
                    process(SimpleBuyIntent.AddNewPaymentMethodRequested(intent.paymentMethod))
                } else {
                    process(SimpleBuyIntent.SelectedPaymentMethodUpdate(intent.paymentMethod))
                }
                null
            }
            is SimpleBuyIntent.MakePayment ->
                interactor.fetchOrder(intent.orderId)
                    .subscribeBy({
                        process(SimpleBuyIntent.ErrorIntent())
                    }, {
                        process(SimpleBuyIntent.OrderPriceUpdated(it.price))
                        if (it.paymentMethodType == PaymentMethodType.PAYMENT_CARD) {
                            handleCardPayment(it)
                        } else {
                            pollForOrderStatus()
                        }
                    })
            is SimpleBuyIntent.UpdatePaymentMethodsAndAddTheFirstEligible -> interactor.eligiblePaymentMethods(
                intent.fiatCurrency, null
            ).subscribeBy(
                onSuccess = {
                    process(it)
                    it.availablePaymentMethods.firstOrNull { it.isEligible }?.let { paymentMethod ->
                        process(SimpleBuyIntent.AddNewPaymentMethodRequested(paymentMethod))
                    }
                },
                onError = {
                    process(SimpleBuyIntent.ErrorIntent())
                }
            )
            is SimpleBuyIntent.ConfirmOrder -> processConfirmOrder(previousState)
            is SimpleBuyIntent.CheckOrderStatus -> interactor.pollForOrderStatus(
                previousState.id ?: throw IllegalStateException("Order Id not available")
            ).subscribeBy(
                onSuccess = {
                    if (it.state == OrderState.FINISHED) {
                        updatePersistingCountersForCompletedOrders()
                        process(SimpleBuyIntent.PaymentSucceeded)
                    } else if (it.state == OrderState.AWAITING_FUNDS || it.state == OrderState.PENDING_EXECUTION) {
                        process(SimpleBuyIntent.CardPaymentPending)
                    } else process(SimpleBuyIntent.ErrorIntent())
                },
                onError = {
                    process(SimpleBuyIntent.ErrorIntent())
                }
            )
            is SimpleBuyIntent.PaymentSucceeded -> {
                interactor.checkTierLevel().map { it.kycState != KycState.VERIFIED_AND_ELIGIBLE }.subscribeBy(
                    onSuccess = {
                        if (it) process(SimpleBuyIntent.UnlockHigherLimits)
                    },
                    onError = {
                        process(SimpleBuyIntent.ErrorIntent())
                    }
                )
            }
            is SimpleBuyIntent.AppRatingShown -> {
                ratingPrefs.hasSeenRatingDialog = true
                null
            }
            else -> null
        }

    private fun processConfirmOrder(previousState: SimpleBuyState) =
        interactor.confirmOrder(
            previousState.id ?: throw IllegalStateException("Order Id not available"),
            previousState.selectedPaymentMethod?.takeIf { it.isBank() }?.concreteId(),
            cardActivators.firstOrNull {
                previousState.selectedPaymentMethod?.partner == it.partner
            }?.paymentAttributes()
        )
            .subscribeBy(
                onSuccess = {
                    val orderCreatedSuccessfully = it.state == OrderState.FINISHED
                    if (orderCreatedSuccessfully) updatePersistingCountersForCompletedOrders()
                    process(SimpleBuyIntent.OrderCreated(it, shouldShowAppRating(orderCreatedSuccessfully)))
                },
                onError = {
                    processErrors(it)
                }
            )

    private fun processErrors(it: Throwable) {
        if (it is NabuApiException) {
            when (it.getErrorCode()) {
                NabuErrorCodes.DailyLimitExceeded -> process(
                    SimpleBuyIntent.ErrorIntent(ErrorState.DailyLimitExceeded)
                )
                NabuErrorCodes.WeeklyLimitExceeded -> process(
                    SimpleBuyIntent.ErrorIntent(ErrorState.WeeklyLimitExceeded)
                )
                NabuErrorCodes.AnnualLimitExceeded -> process(
                    SimpleBuyIntent.ErrorIntent(ErrorState.YearlyLimitExceeded)
                )
                NabuErrorCodes.PendingOrdersLimitReached -> process(
                    SimpleBuyIntent.ErrorIntent(ErrorState.ExistingPendingOrder)
                )
                else -> process(SimpleBuyIntent.ErrorIntent())
            }
        } else {
            process(SimpleBuyIntent.ErrorIntent())
        }
    }

    private fun updatePersistingCountersForCompletedOrders() {
        ratingPrefs.preRatingActionCompletedTimes = ratingPrefs.preRatingActionCompletedTimes + 1
        prefs.hasCompletedAtLeastOneBuy = true
    }

    private fun shouldShowAppRating(orderCreatedSuccessFully: Boolean): Boolean =
        ratingPrefs.preRatingActionCompletedTimes >= COMPLETED_ORDERS_BEFORE_SHOWING_APP_RATING &&
            !ratingPrefs.hasSeenRatingDialog && orderCreatedSuccessFully

    private fun pollForOrderStatus() {
        process(SimpleBuyIntent.CheckOrderStatus)
    }

    private fun handleCardPayment(order: BuySellOrder) {
        order.attributes?.everypay?.let { attrs ->
            if (attrs.paymentState == EverypayPaymentAttrs.WAITING_3DS &&
                order.state == OrderState.AWAITING_FUNDS
            ) {
                process(
                    SimpleBuyIntent.Open3dsAuth(
                        attrs.paymentLink,
                        EverypayCardActivator.redirectUrl
                    )
                )
                process(SimpleBuyIntent.ResetEveryPayAuth)
            } else {
                process(SimpleBuyIntent.CheckOrderStatus)
            }
        } ?: kotlin.run {
            process(SimpleBuyIntent.ErrorIntent()) // todo handle case of partner not supported
        }
    }

    override fun onStateUpdate(s: SimpleBuyState) {
        prefs.updateSimpleBuyState(gson.toJson(s))
    }

    companion object {
        const val COMPLETED_ORDERS_BEFORE_SHOWING_APP_RATING = 1
    }
}