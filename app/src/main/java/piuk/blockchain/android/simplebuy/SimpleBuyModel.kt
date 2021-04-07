package piuk.blockchain.android.simplebuy

import com.blockchain.extensions.exhaustive
import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.datamanagers.ApprovalErrorStatus
import com.blockchain.nabu.datamanagers.BuySellOrder
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.UndefinedPaymentMethod
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.BankPartner.Companion.YAPILY_DEEPLINK_PAYMENT_APPROVAL_URL
import com.blockchain.nabu.models.responses.nabu.NabuApiException
import com.blockchain.nabu.models.responses.nabu.NabuErrorCodes
import com.blockchain.nabu.models.responses.simplebuy.EverypayPaymentAttrs
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyConfirmationAttributes
import com.blockchain.preferences.RatingPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.google.gson.Gson
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
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
                        if (it.attributes != null) {
                            handleOrderAttrs(it)
                        } else {
                            pollForOrderStatus()
                        }
                    })
            is SimpleBuyIntent.GetAuthorisationUrl ->
                interactor.pollForAuthorisationUrl(intent.orderId)
                    .subscribeBy(
                        onSuccess = { order ->
                            order.attributes?.authorisationUrl?.let {
                                handleBankAuthorisationPayment(order.paymentMethodId, it)
                            }
                        },
                        onError = {
                            process(SimpleBuyIntent.ErrorIntent())
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
                        process(SimpleBuyIntent.PaymentPending)
                    } else {
                        if (it.approvalErrorStatus != ApprovalErrorStatus.NONE) {
                            handleApprovalErrorState(it)
                        } else {
                            process(SimpleBuyIntent.ErrorIntent())
                        }
                    }
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

    private fun handleApprovalErrorState(it: BuySellOrder) {
        when (it.approvalErrorStatus) {
            ApprovalErrorStatus.FAILED -> process(
                SimpleBuyIntent.ErrorIntent(ErrorState.ApprovedBankFailed)
            )
            ApprovalErrorStatus.REJECTED -> process(
                SimpleBuyIntent.ErrorIntent(ErrorState.ApprovedBankRejected)
            )
            ApprovalErrorStatus.DECLINED -> process(
                SimpleBuyIntent.ErrorIntent(ErrorState.ApprovedBankDeclined)
            )
            ApprovalErrorStatus.EXPIRED -> process(
                SimpleBuyIntent.ErrorIntent(ErrorState.ApprovedBankExpired)
            )
            ApprovalErrorStatus.UNKNOWN -> process(
                SimpleBuyIntent.ErrorIntent(ErrorState.ApprovedGenericError)
            )
            ApprovalErrorStatus.NONE -> {
                // do nothing
            }
        }.exhaustive
    }

    private fun handleOrderAttrs(order: BuySellOrder) {
        order.attributes?.everypay?.let {
            handleCardPayment(order)
        } ?: kotlin.run {
            if (!order.fiat.isOpenBankingCurrency()) {
                process(SimpleBuyIntent.CheckOrderStatus)
            } else {
                order.attributes?.authorisationUrl?.let {
                    handleBankAuthorisationPayment(order.paymentMethodId, it)
                } ?: process(SimpleBuyIntent.GetAuthorisationUrl(order.id))
            }
        }
    }

    private fun FiatValue.isOpenBankingCurrency() =
        this.currencyCode == "EUR" || this.currencyCode == "GBP"

    private fun processConfirmOrder(previousState: SimpleBuyState): Disposable {
        val isBankPayment = previousState.selectedPaymentMethod?.isBank()
        return interactor.confirmOrder(
            previousState.id ?: throw IllegalStateException("Order Id not available"),
            previousState.selectedPaymentMethod?.takeIf { it.isBank() }?.concreteId(),
            if (isBankPayment == true) {
                SimpleBuyConfirmationAttributes(callback = YAPILY_DEEPLINK_PAYMENT_APPROVAL_URL)
            } else {
                cardActivators.firstOrNull {
                    previousState.selectedPaymentMethod?.partner == it.partner
                }?.paymentAttributes()
            },
            isBankPayment
        ).subscribeBy(
            onSuccess = {
                val orderCreatedSuccessfully = it.state == OrderState.FINISHED
                if (orderCreatedSuccessfully) {
                    updatePersistingCountersForCompletedOrders()
                }
                process(SimpleBuyIntent.OrderCreated(it, shouldShowAppRating(orderCreatedSuccessfully)))
            },
            onError = {
                processOrderErrors(it)
            }
        )
    }

    private fun processOrderErrors(it: Throwable) {
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

    private fun handleBankAuthorisationPayment(
        paymentMethodId: String,
        authorisationUrl: String
    ) {
        disposables += interactor.getLinkedBankInfo(paymentMethodId).subscribeBy(
            onSuccess = { linkedBank ->
                process(SimpleBuyIntent.AuthorisePaymentExternalUrl(authorisationUrl, linkedBank))
            },
            onError = {
                process(SimpleBuyIntent.ErrorIntent())
            }
        )
    }

    override fun onStateUpdate(s: SimpleBuyState) {
        prefs.updateSimpleBuyState(gson.toJson(s))
    }

    companion object {
        const val COMPLETED_ORDERS_BEFORE_SHOWING_APP_RATING = 1
    }
}