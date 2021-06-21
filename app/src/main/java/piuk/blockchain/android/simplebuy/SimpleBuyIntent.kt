package piuk.blockchain.android.simplebuy

import com.blockchain.nabu.datamanagers.BuySellOrder
import com.blockchain.nabu.datamanagers.BuySellPairs
import com.blockchain.nabu.datamanagers.CustodialQuote
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.Partner
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.datamanagers.TransferLimits
import com.blockchain.nabu.datamanagers.UndefinedPaymentMethod
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.LinkBankTransfer
import com.blockchain.nabu.models.data.LinkedBank
import com.blockchain.nabu.models.data.RecurringBuyFrequency
import com.blockchain.nabu.models.data.RecurringBuyState
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import piuk.blockchain.android.cards.EverypayAuthOptions
import piuk.blockchain.android.ui.base.mvi.MviIntent
import piuk.blockchain.android.ui.sell.ExchangePriceWithDelta
import java.math.BigInteger

sealed class SimpleBuyIntent : MviIntent<SimpleBuyState> {

    override fun isValidFor(oldState: SimpleBuyState): Boolean {
        return oldState.errorState == null
    }

    override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
        oldState

    class NewCryptoCurrencySelected(val currency: CryptoCurrency) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            if (oldState.selectedCryptoCurrency == currency) oldState else
                oldState.copy(
                    selectedCryptoCurrency = currency,
                    amount = null,
                    exchangePriceWithDelta = null
                )
    }

    class AmountUpdated(private val amount: FiatValue) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(amount = amount)
    }

    object ResetLinkBankTransfer : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(linkBankTransfer = null, newPaymentMethodToBeAdded = null)
    }

    class OrderPriceUpdated(private val price: FiatValue?) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(orderExchangePrice = price, isLoading = false)
    }

    class Open3dsAuth(private val paymentLink: String, private val exitLink: String) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(everypayAuthOptions = EverypayAuthOptions(paymentLink, exitLink))
    }

    class AuthorisePaymentExternalUrl(private val url: String, private val linkedBank: LinkedBank) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(authorisePaymentUrl = url, linkedBank = linkedBank)
    }

    class ExchangePriceWithDeltaUpdated(private val exchangePriceWithDelta: ExchangePriceWithDelta) :
        SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(exchangePriceWithDelta = exchangePriceWithDelta)
    }

    class PaymentMethodChangeRequested(val paymentMethod: PaymentMethod) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState = oldState
    }

    class FetchPaymentDetails(val fiatCurrency: String, val selectedPaymentMethodId: String) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState = oldState.copy(isLoading = true)
    }

    class PaymentMethodsUpdated(
        val availablePaymentMethods: List<PaymentMethod>,
        private val canAddCard: Boolean,
        private val canLinkFunds: Boolean,
        private val canLinkBank: Boolean,
        private val preselectedId: String? // pass this value if you want to preselect one
    ) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            val selectedPaymentMethodId = selectedMethodId(oldState.selectedPaymentMethod?.id)
            val selectedPaymentMethod = availablePaymentMethods.firstOrNull {
                it.id == selectedPaymentMethodId
            }

            val type = when (selectedPaymentMethod) {
                is PaymentMethod.Card -> PaymentMethodType.PAYMENT_CARD
                is PaymentMethod.Funds -> PaymentMethodType.FUNDS
                is PaymentMethod.Bank -> PaymentMethodType.BANK_TRANSFER
                is UndefinedPaymentMethod -> selectedPaymentMethod.paymentMethodType
                else -> PaymentMethodType.UNKNOWN
            }

            return oldState.copy(
                isLoading = false,
                selectedPaymentMethod = selectedPaymentMethod?.let {
                    SelectedPaymentMethod(
                        selectedPaymentMethod.id,
                        (selectedPaymentMethod as? PaymentMethod.Card)?.partner,
                        selectedPaymentMethod.detailedLabel(),
                        type,
                        selectedPaymentMethod.isEligible
                    )
                },
                paymentOptions = PaymentOptions(
                    availablePaymentMethods = availablePaymentMethods,
                    canAddCard = canAddCard,
                    canLinkFunds = canLinkFunds,
                    canLinkBank = canLinkBank
                )
            )
        }

        // If no preselected Id, we want the first eligible, if none present, check if available is only 1 and
        // preselect it. Otherwise, don't preselect anything
        private fun selectedMethodId(oldStateId: String?): String? =
            when {
                preselectedId != null -> availablePaymentMethods.firstOrNull { it.id == preselectedId }?.id
                oldStateId != null -> availablePaymentMethods.firstOrNull { it.id == oldStateId }?.id
                else -> {
                    // we skip undefined funds cause this payment method should trigger a bottom sheet
                    // and it should always be actioned before
                    val paymentMethodsThatCanBePreselected =
                        availablePaymentMethods.filter { it !is PaymentMethod.UndefinedFunds }
                    paymentMethodsThatCanBePreselected.firstOrNull { it.isEligible && it.canUsedForPaying() }?.id
                        ?: paymentMethodsThatCanBePreselected.firstOrNull { it.isEligible }?.id
                        ?: paymentMethodsThatCanBePreselected.firstOrNull()?.id
                }
            }
    }

    class SelectedPaymentMethodUpdate(
        private val paymentMethod: PaymentMethod
    ) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                selectedPaymentMethod = SelectedPaymentMethod(
                    paymentMethod.id,
                    // no partner for bank transfer or ui label. Ui label for bank transfer is coming from resources
                    (paymentMethod as? PaymentMethod.Card)?.partner,
                    paymentMethod.detailedLabel(),
                    when (paymentMethod) {
                        is PaymentMethod.UndefinedBankTransfer -> PaymentMethodType.BANK_TRANSFER
                        is PaymentMethod.UndefinedCard -> PaymentMethodType.PAYMENT_CARD
                        is PaymentMethod.Bank -> PaymentMethodType.BANK_TRANSFER
                        is PaymentMethod.Funds -> PaymentMethodType.FUNDS
                        is PaymentMethod.UndefinedFunds -> PaymentMethodType.FUNDS
                        else -> PaymentMethodType.PAYMENT_CARD
                    },
                    paymentMethod.isEligible
                )
            )
    }

    class UpdateExchangePriceWithDelta(val currency: CryptoCurrency) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(exchangePriceWithDelta = null)
    }

    object BuyButtonClicked : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(confirmationActionRequested = true, orderState = OrderState.INITIALISED)
    }

    object LinkBankTransferRequested : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState = oldState.copy(
            newPaymentMethodToBeAdded = null
        )
    }

    object TryToLinkABankTransfer : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            return oldState.copy(isLoading = true)
        }
    }

    object ClearAnySelectedPaymentMethods : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                selectedPaymentMethod = null
            )
    }

    data class FiatCurrencyUpdated(private val fiatCurrency: String) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(fiatCurrency = fiatCurrency, amount = null)
    }

    data class UpdatedBuyLimitsAndSupportedCryptoCurrencies(
        val buySellPairs: BuySellPairs,
        private val selectedCryptoCurrency: CryptoCurrency?,
        private val transferLimits: TransferLimits?
    ) : SimpleBuyIntent() {

        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            val supportedPairsAndLimits = buySellPairs.pairs.filter { it.fiatCurrency == oldState.fiatCurrency }

            if (supportedPairsAndLimits.isEmpty()) {
                return oldState.copy(errorState = ErrorState.NoAvailableCurrenciesToTrade)
            }

            return oldState.copy(
                supportedPairsAndLimits = supportedPairsAndLimits,
                selectedCryptoCurrency = selectedCryptoCurrency,
                transferLimits = transferLimits
            )
        }
    }

    data class SupportedCurrenciesUpdated(private val currencies: List<String>) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(supportedFiatCurrencies = currencies)
    }

    data class WithdrawLocksTimeUpdated(private val time: BigInteger = BigInteger.ZERO) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            return oldState.copy(withdrawalLockPeriod = time)
        }
    }

    data class QuoteUpdated(private val custodialQuote: CustodialQuote) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            return oldState.copy(custodialQuote = custodialQuote)
        }
    }

    data class FetchBuyLimits(val fiatCurrency: String, val cryptoCurrency: CryptoCurrency) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(fiatCurrency = fiatCurrency, selectedCryptoCurrency = cryptoCurrency)
    }

    data class FlowCurrentScreen(val flowScreen: FlowScreen) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState = oldState.copy(currentScreen = flowScreen)
    }

    data class FetchSuggestedPaymentMethod(val fiatCurrency: String, val selectedPaymentMethodId: String? = null) :
        SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(paymentOptions = PaymentOptions(), selectedPaymentMethod = null)
    }

    object FetchSupportedFiatCurrencies : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(supportedFiatCurrencies = emptyList())
    }

    object CancelOrder : SimpleBuyIntent() {
        override fun isValidFor(oldState: SimpleBuyState) = true
    }

    object CancelOrderAndResetAuthorisation : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                authorisePaymentUrl = null,
                linkedBank = null
            )
    }

    object ClearState : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            SimpleBuyState()

        override fun isValidFor(oldState: SimpleBuyState): Boolean {
            return oldState.orderState < OrderState.PENDING_CONFIRMATION ||
                oldState.orderState > OrderState.PENDING_EXECUTION
        }
    }

    object ConfirmOrder : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(confirmationActionRequested = true, isLoading = true)
    }

    object FetchWithdrawLockTime : SimpleBuyIntent()

    object NavigationHandled : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(confirmationActionRequested = false, newPaymentMethodToBeAdded = null)
    }

    object KycStarted : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                kycStartedButNotCompleted = true,
                currentScreen = FlowScreen.KYC,
                kycVerificationState = null
            )
    }

    class ErrorIntent(private val error: ErrorState = ErrorState.GenericError) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(errorState = error, isLoading = false, confirmationActionRequested = false)

        override fun isValidFor(oldState: SimpleBuyState): Boolean {
            return true
        }
    }

    object KycCompleted : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(kycStartedButNotCompleted = false)
    }

    object FetchKycState : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(kycVerificationState = KycState.PENDING)
    }

    object FetchQuote : SimpleBuyIntent()

    class KycStateUpdated(val kycState: KycState) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(kycVerificationState = kycState)
    }

    class BankLinkProcessStarted(private val bankTransfer: LinkBankTransfer) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            return oldState.copy(
                linkBankTransfer = bankTransfer,
                confirmationActionRequested = false,
                newPaymentMethodToBeAdded = null,
                isLoading = false
            )
        }
    }

    object OrderCanceled : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            SimpleBuyState(orderState = OrderState.CANCELED)
    }

    class OrderCreated(
        private val buyOrder: BuySellOrder,
        private val showInAppRating: Boolean = false,
        private val recurringBuyState: RecurringBuyState = RecurringBuyState.UNINITIALISED
    ) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                orderState = buyOrder.state,
                expirationDate = buyOrder.expires,
                id = buyOrder.id,
                fee = buyOrder.fee,
                orderValue = buyOrder.orderValue as CryptoValue,
                orderExchangePrice = buyOrder.price,
                paymentSucceeded = buyOrder.state == OrderState.FINISHED,
                isLoading = false,
                showRating = showInAppRating,
                recurringBuyState = recurringBuyState
            )
    }

    object AppRatingShown : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(showRating = false)
    }

    class UpdateSelectedPaymentCard(
        private val id: String,
        private val label: String?,
        private val partner: Partner,
        private val isEligible: Boolean
    ) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(
                selectedPaymentMethod = SelectedPaymentMethod(
                    id, partner, label, PaymentMethodType.PAYMENT_CARD, isEligible
                )
            )
    }

    object ClearError : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(errorState = null)

        override fun isValidFor(oldState: SimpleBuyState): Boolean {
            return oldState.errorState != null
        }
    }

    object ResetEveryPayAuth : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(everypayAuthOptions = null)
    }

    object CancelOrderIfAnyAndCreatePendingOne : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(isLoading = true)

        override fun isValidFor(oldState: SimpleBuyState): Boolean {
            return oldState.selectedCryptoCurrency != null &&
                oldState.order.amount != null &&
                oldState.orderState != OrderState.AWAITING_FUNDS &&
                oldState.orderState != OrderState.PENDING_EXECUTION
        }
    }

    class MakePayment(val orderId: String) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(isLoading = true)
    }

    class GetAuthorisationUrl(val orderId: String) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(isLoading = true)
    }

    object CheckOrderStatus : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(isLoading = true)
    }

    object PaymentSucceeded : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(paymentSucceeded = true, isLoading = false)
    }

    object UnlockHigherLimits : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(shouldShowUnlockHigherFunds = true)
    }

    object PaymentPending : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(paymentPending = true, isLoading = false)
    }

    class AddNewPaymentMethodRequested(private val paymentMethod: PaymentMethod) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(newPaymentMethodToBeAdded = paymentMethod)
    }

    class UpdatePaymentMethodsAndAddTheFirstEligible(val fiatCurrency: String) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(paymentOptions = PaymentOptions(), selectedPaymentMethod = null)
    }

    object AddNewPaymentMethodHandled : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(newPaymentMethodToBeAdded = null)
    }

    class RecurringBuyIntervalUpdated(private val recurringBuyFrequency: RecurringBuyFrequency) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(recurringBuyFrequency = recurringBuyFrequency)
    }

    class RecurringBuyEligibilityUpdated(private val eligibleMethods: List<PaymentMethodType>) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(recurringBuyEligiblePaymentMethods = eligibleMethods)
    }
}