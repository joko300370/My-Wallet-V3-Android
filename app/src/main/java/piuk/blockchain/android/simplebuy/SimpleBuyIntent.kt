package piuk.blockchain.android.simplebuy

import com.blockchain.swap.nabu.datamanagers.BankAccount
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.swap.nabu.datamanagers.Quote
import com.blockchain.swap.nabu.datamanagers.SimpleBuyPairs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import piuk.blockchain.android.ui.base.mvi.MviIntent
import java.util.Date

sealed class SimpleBuyIntent : MviIntent<SimpleBuyState> {

    override fun isValidFor(oldState: SimpleBuyState): Boolean {
        return oldState.errorState == null
    }

    class NewCryptoCurrencySelected(val currency: CryptoCurrency) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            if (oldState.selectedCryptoCurrency == currency) oldState else
                oldState.copy(selectedCryptoCurrency = currency, enteredAmount = "")
    }

    class EnteredAmount(private val amount: String) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(enteredAmount = amount)
    }

    object BuyButtonClicked : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(confirmationActionRequested = true, orderState = OrderState.INITIALISED)
    }

    data class UpdatedBuyLimitsAndSupportedCryptoCurrencies(val simpleBuyPairs: SimpleBuyPairs) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            val supportedPairsAndLimits = simpleBuyPairs.pairs.filter { it.fiatCurrency == oldState.currency }

            if (supportedPairsAndLimits.isEmpty()) {
                return oldState.copy(errorState = ErrorState.NoAvailableCurrenciesToTrade)
            }

            val selectedCryptoCurrency = oldState.selectedCryptoCurrency ?: simpleBuyPairs.pairs.firstOrNull {
                it.fiatCurrency == oldState.currency
            }?.cryptoCurrency

            val minValueForSelectedPair = supportedPairsAndLimits.firstOrNull { pairs ->
                pairs.fiatCurrency == oldState.currency &&
                        pairs.cryptoCurrency == selectedCryptoCurrency
            }?.buyLimits?.minLimit(oldState.currency)?.valueMinor

            val maxValueForSelectedPair = supportedPairsAndLimits.firstOrNull { pairs ->
                pairs.fiatCurrency == oldState.currency &&
                        pairs.cryptoCurrency == selectedCryptoCurrency
            }?.buyLimits?.maxLimit(oldState.currency)?.valueMinor

            return oldState.copy(
                supportedPairsAndLimits = supportedPairsAndLimits,
                selectedCryptoCurrency = selectedCryptoCurrency,
                predefinedAmounts = oldState.predefinedAmounts.filter {
                    it.valueMinor >= (minValueForSelectedPair ?: 0) && it.valueMinor <= (maxValueForSelectedPair ?: 0)
                }
            )
        }
    }

    data class UpdatedPredefinedAmounts(private val amounts: List<FiatValue>) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            return oldState.copy(predefinedAmounts = amounts.filter {
                val isBiggerThanMin = it.valueMinor >= oldState.minAmount?.valueMinor ?: return@filter true
                val isSmallerThanMax = it.valueMinor <= oldState.maxAmount?.valueMinor ?: return@filter true
                isBiggerThanMin && isSmallerThanMax
            })
        }
    }

    data class BankAccountUpdated(private val account: BankAccount) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            return oldState.copy(bankAccount = account)
        }
    }

    data class QuoteUpdated(private val quote: Quote) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            return oldState.copy(quote = quote)
        }
    }

    data class FetchBuyLimits(val currency: String) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState = oldState.copy(currency = currency)
    }

    data class FlowCurrentScreen(val flowScreen: FlowScreen) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState = oldState.copy(currentScreen = flowScreen)
    }

    data class FetchPredefinedAmounts(val currency: String) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(currency = currency, predefinedAmounts = emptyList())
    }

    object CancelOrder : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState
    }

    object ClearState : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            SimpleBuyState()

        override fun isValidFor(oldState: SimpleBuyState): Boolean {
            return true
        }
    }

    object ConfirmOrder : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(confirmationActionRequested = true)
    }

    object FetchBankAccount : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState
    }

    object ConfirmationHandled : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(confirmationActionRequested = false)
    }

    object KycStarted : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(kycStartedButNotCompleted = true, currentScreen = FlowScreen.KYC, kycVerificationState = null)
    }

    class ErrorIntent(private val error: ErrorState = ErrorState.GenericError) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(errorState = error)

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

    object FetchQuote : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState
    }

    class KycStateUpdated(val kycState: KycState) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(kycVerificationState = kycState)
    }

    object OrderCanceled : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            SimpleBuyState(orderState = OrderState.CANCELED)
    }

    class OrderCreated(private val id: String, private val expirationDate: Date, private val orderState: OrderState) :
        SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(orderState = orderState, expirationDate = expirationDate, id = id)
    }

    object ClearError : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(errorState = null)
    }
}