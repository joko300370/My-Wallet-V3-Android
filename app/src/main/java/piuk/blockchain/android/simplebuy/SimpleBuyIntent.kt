package piuk.blockchain.android.simplebuy

import com.blockchain.swap.nabu.datamanagers.BankAccount
import com.blockchain.swap.nabu.datamanagers.BuySellOrder
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.swap.nabu.datamanagers.Partner
import com.blockchain.swap.nabu.datamanagers.PaymentMethod
import com.blockchain.swap.nabu.datamanagers.Quote
import com.blockchain.swap.nabu.datamanagers.BuySellPairs
import com.blockchain.swap.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import piuk.blockchain.android.cards.EverypayAuthOptions
import piuk.blockchain.android.ui.base.mvi.MviIntent
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
                oldState.copy(selectedCryptoCurrency = currency, amount = null, exchangePrice = null)
    }

    class AmountUpdated(private val amount: FiatValue) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(amount = amount)
    }

    class OrderPriceUpdated(private val price: FiatValue?) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(orderExchangePrice = price, isLoading = false)
    }

    class Open3dsAuth(private val paymentLink: String, private val exitLink: String) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(everypayAuthOptions = EverypayAuthOptions(paymentLink, exitLink))
    }

    class ExchangeRateUpdated(private val price: FiatValue) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(exchangePrice = price)
    }

    class PaymentMethodsUpdated(
        private val availablePaymentMethods: List<PaymentMethod>,
        private val canAddCard: Boolean,
        private val canLinkFunds: Boolean,
        private val preselectedId: String? = null // pass this value if you want to preselect one
    ) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            val selectedPaymentMethodId =
                selectedMethodId(oldState.selectedPaymentMethod?.id) ?: availablePaymentMethods[0].id
            val selectedPaymentMethod = availablePaymentMethods.firstOrNull {
                it.id == selectedPaymentMethodId
            }

            val type = when (selectedPaymentMethod) {
                is PaymentMethod.Card -> PaymentMethodType.PAYMENT_CARD
                is PaymentMethod.BankTransfer -> PaymentMethodType.BANK_ACCOUNT
                is PaymentMethod.Funds -> PaymentMethodType.FUNDS
                else -> PaymentMethodType.UNKNOWN
            }

            return oldState.copy(
                selectedPaymentMethod = SelectedPaymentMethod(
                    selectedPaymentMethodId,
                    (selectedPaymentMethod as? PaymentMethod.Card)?.partner,
                    (selectedPaymentMethod as? PaymentMethod.Card)?.uiLabelWithDigits() ?: "",
                    type
                ),
                paymentOptions = PaymentOptions(
                    availablePaymentMethods = availablePaymentMethods,
                    canAddCard = canAddCard,
                    canLinkFunds = canLinkFunds
                )
            )
        }

        private fun selectedMethodId(oldStateId: String?): String? =
            when {
                preselectedId != null -> availablePaymentMethods.firstOrNull { it.id == preselectedId }?.id
                oldStateId != null -> availablePaymentMethods.firstOrNull { it.id == oldStateId }?.id
                else -> availablePaymentMethods[0].id
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
                    (paymentMethod as? PaymentMethod.Card)?.uiLabelWithDigits() ?: "",
                    when (paymentMethod) {
                        is PaymentMethod.BankTransfer -> PaymentMethodType.BANK_ACCOUNT
                        is PaymentMethod.Funds -> PaymentMethodType.FUNDS
                        is PaymentMethod.UndefinedFunds -> PaymentMethodType.FUNDS
                        else -> PaymentMethodType.PAYMENT_CARD
                    }
                ))
    }

    class UpdateExchangeRate(val currency: CryptoCurrency) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(exchangePrice = null)
    }

    object BuyButtonClicked : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(confirmationActionRequested = true, orderState = OrderState.INITIALISED)
    }

    class FiatCurrencyUpdated(private val fiatCurrency: String) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(fiatCurrency = fiatCurrency, amount = null)
    }

    data class UpdatedBuyLimitsAndSupportedCryptoCurrencies(
        val buySellPairs: BuySellPairs,
        private val selectedCryptoCurrency: CryptoCurrency?
    ) : SimpleBuyIntent() {

        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            val supportedPairsAndLimits = buySellPairs.pairs.filter { it.fiatCurrency == oldState.fiatCurrency }

            if (supportedPairsAndLimits.isEmpty()) {
                return oldState.copy(errorState = ErrorState.NoAvailableCurrenciesToTrade)
            }

            val minValueForSelectedPair = supportedPairsAndLimits.firstOrNull { pairs ->
                pairs.fiatCurrency == oldState.fiatCurrency &&
                        pairs.cryptoCurrency == selectedCryptoCurrency
            }?.buyLimits?.minLimit(oldState.fiatCurrency)?.valueMinor

            val maxValueForSelectedPair = supportedPairsAndLimits.firstOrNull { pairs ->
                pairs.fiatCurrency == oldState.fiatCurrency &&
                        pairs.cryptoCurrency == selectedCryptoCurrency
            }?.buyLimits?.maxLimit(oldState.fiatCurrency)?.valueMinor

            return oldState.copy(
                supportedPairsAndLimits = supportedPairsAndLimits,
                selectedCryptoCurrency = selectedCryptoCurrency,
                predefinedAmounts = oldState.predefinedAmounts.filter {
                    it.valueMinor >= (minValueForSelectedPair ?: 0) && it.valueMinor <= (maxValueForSelectedPair
                        ?: 0)
                }
            )
        }
    }

    data class SupportedCurrenciesUpdated(private val currencies: List<String>) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(supportedFiatCurrencies = currencies)
    }

    data class UpdatedPredefinedAmounts(private val amounts: List<FiatValue>) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            return oldState.copy(predefinedAmounts = amounts.filter {
                val isBiggerThanMin = it.valueMinor >= oldState.minFiatAmount.valueMinor
                val isSmallerThanMax = it.valueMinor <= oldState.maxFiatAmount.valueMinor
                isBiggerThanMin && isSmallerThanMax
            })
        }
    }

    data class BankAccountUpdated(private val account: BankAccount) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            return oldState.copy(bankAccount = account)
        }
    }

    data class WithdrawLocksTimeUpdated(private val time: BigInteger = BigInteger.ZERO) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            return oldState.copy(withdrawalLockPeriod = time)
        }
    }

    data class QuoteUpdated(private val quote: Quote) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            return oldState.copy(quote = quote)
        }
    }

    data class FetchBuyLimits(val fiatCurrency: String, val cryptoCurrency: CryptoCurrency) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(fiatCurrency = fiatCurrency, selectedCryptoCurrency = cryptoCurrency)
    }

    data class FlowCurrentScreen(val flowScreen: FlowScreen) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState = oldState.copy(currentScreen = flowScreen)
    }

    data class FetchPredefinedAmounts(val fiatCurrency: String) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(fiatCurrency = fiatCurrency, predefinedAmounts = emptyList())
    }

    data class FetchSuggestedPaymentMethod(val fiatCurrency: String, val selectedPaymentMethodId: String? = null) :
        SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(paymentOptions = PaymentOptions())
    }

    object FetchSupportedFiatCurrencies : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(supportedFiatCurrencies = emptyList())
    }

    object CancelOrder : SimpleBuyIntent() {
        override fun isValidFor(oldState: SimpleBuyState) = true
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

    object FetchBankAccount : SimpleBuyIntent()

    object FetchWithdrawLockTime : SimpleBuyIntent()

    object NavigationHandled : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(confirmationActionRequested = false, depositFundsRequested = false)
    }

    object KycStarted : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(kycStartedButNotCompleted = true,
                currentScreen = FlowScreen.KYC,
                kycVerificationState = null)
    }

    class ErrorIntent(private val error: ErrorState = ErrorState.GenericError) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(errorState = error, isLoading = false)

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

    object OrderCanceled : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            SimpleBuyState(orderState = OrderState.CANCELED)
    }

    class OrderCreated(
        private val buyOrder: BuySellOrder
    ) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(orderState = buyOrder.state,
                expirationDate = buyOrder.expires,
                id = buyOrder.id,
                fee = buyOrder.fee,
                orderValue = buyOrder.orderValue as CryptoValue,
                orderExchangePrice = buyOrder.price,
                paymentSucceeded = buyOrder.state == OrderState.FINISHED,
                isLoading = false
            )
    }

    class UpdateSelectedPaymentMethod(
        private val id: String,
        private val label: String?,
        private val partner: Partner?,
        private val type: PaymentMethodType
    ) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(selectedPaymentMethod = SelectedPaymentMethod(id, partner, label, type))
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

    object CheckOrderStatus : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(isLoading = true)
    }

    object CardPaymentSucceeded : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(paymentSucceeded = true, isLoading = false)
    }

    object CardPaymentPending : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(paymentPending = true, isLoading = false)
    }

    object DepositFundsRequested : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(depositFundsRequested = true)
    }
}
