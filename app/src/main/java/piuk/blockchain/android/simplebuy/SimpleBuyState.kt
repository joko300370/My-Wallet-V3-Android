package piuk.blockchain.android.simplebuy

import com.blockchain.swap.nabu.datamanagers.BankAccount
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.swap.nabu.datamanagers.Partner
import com.blockchain.swap.nabu.datamanagers.PaymentMethod
import com.blockchain.swap.nabu.datamanagers.Quote
import com.blockchain.swap.nabu.datamanagers.SimpleBuyPair
import com.blockchain.swap.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import piuk.blockchain.android.cards.EverypayAuthOptions
import piuk.blockchain.android.ui.base.mvi.MviState
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.toCrypto
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import java.util.Date
import java.util.regex.Pattern

/**
 * This is an object that gets serialized with Gson so any properties that we don't
 * want to get serialized should be tagged as @Transient
 *
 */
data class SimpleBuyState(
    val id: String? = null,
    val supportedPairsAndLimits: List<SimpleBuyPair>? = null,
    private val amount: FiatValue? = null,
    val fiatCurrency: String = "USD",
    val predefinedAmounts: List<FiatValue> = emptyList(),
    val selectedCryptoCurrency: CryptoCurrency? = null,
    val orderState: OrderState = OrderState.UNINITIALISED,
    private val expirationDate: Date? = null,
    val quote: Quote? = null,
    val kycStartedButNotCompleted: Boolean = false,
    val kycVerificationState: KycState? = null,
    val bankAccount: BankAccount? = null,
    val currentScreen: FlowScreen = FlowScreen.INTRO,
    val selectedPaymentMethod: SelectedPaymentMethod? = null,
    val orderExchangePrice: FiatValue? = null,
    val orderValue: CryptoValue? = null,
    val fee: FiatValue? = null,
    @Transient val paymentOptions: PaymentOptions = PaymentOptions(),
    val supportedFiatCurrencies: List<String> = emptyList(),
    @Transient val errorState: ErrorState? = null,
    @Transient val exchangePrice: FiatValue? = null,
    @Transient val isLoading: Boolean = false,
    @Transient val everypayAuthOptions: EverypayAuthOptions? = null,
    val paymentSucceeded: Boolean = false,
    @Transient val paymentPending: Boolean = false,
    // we use this flag to avoid navigating back and forth, reset after navigating
    @Transient val confirmationActionRequested: Boolean = false
) : MviState {

    @delegate:Transient
    val order: SimpleBuyOrder by unsafeLazy {
        SimpleBuyOrder(
            orderState,
            amount,
            expirationDate,
            quote
        )
    }

    @Transient
    private val pattern = Pattern.compile("-?\\d+(\\.\\d+)?")

    @delegate:Transient
    val availableCryptoCurrencies: List<CryptoCurrency> by unsafeLazy {
        supportedPairsAndLimits?.filter { it.fiatCurrency == fiatCurrency }?.map { it.cryptoCurrency }?.distinct()
            ?: emptyList()
    }

    @delegate:Transient
    val selectedPaymentMethodDetails: PaymentMethod? by unsafeLazy {
        selectedPaymentMethod?.id?.let { id ->
            paymentOptions.availablePaymentMethods.firstOrNull { it.id == id }
        }
    }

    @delegate:Transient
    val maxFiatAmount: FiatValue by unsafeLazy {
        val maxPairBuyLimit = maxPairsLimit() ?: return@unsafeLazy FiatValue.fromMinor(fiatCurrency, Long.MAX_VALUE)

        val maxPaymentMethodLimit = selectedPaymentMethodDetails.maxLimit()

        maxPaymentMethodLimit?.let {
            FiatValue.fromMinor(fiatCurrency, it.coerceAtMost(maxPairBuyLimit))
        } ?: FiatValue.fromMinor(fiatCurrency, maxPairBuyLimit)
    }

    @delegate:Transient
    val minFiatAmount: FiatValue by unsafeLazy {
        val minPairBuyLimit = minPairsLimit() ?: return@unsafeLazy FiatValue.zero(fiatCurrency)

        val minPaymentMethodLimit = selectedPaymentMethodDetails.minLimit()

        minPaymentMethodLimit?.let {
            FiatValue.fromMinor(fiatCurrency, it.coerceAtLeast(minPairBuyLimit))
        } ?: FiatValue.fromMinor(fiatCurrency, minPairBuyLimit)
    }

    fun maxCryptoAmount(exchangeRateDataManager: ExchangeRateDataManager): CryptoValue? =
        selectedCryptoCurrency?.let {
            maxFiatAmount.toCrypto(exchangeRateDataManager, it)
        }

    fun minCryptoAmount(exchangeRateDataManager: ExchangeRateDataManager): CryptoValue? =
        selectedCryptoCurrency?.let {
            minFiatAmount.toCrypto(exchangeRateDataManager, it)
        }

    private fun PaymentMethod?.maxLimit(): Long? = this?.limits?.max?.valueMinor
    private fun PaymentMethod?.minLimit(): Long? = this?.limits?.min?.valueMinor

    private fun maxPairsLimit(): Long? = supportedPairsAndLimits?.find {
        it.cryptoCurrency == selectedCryptoCurrency && it.fiatCurrency == fiatCurrency
    }?.buyLimits?.maxLimit(fiatCurrency)?.valueMinor

    private fun minPairsLimit(): Long? = supportedPairsAndLimits?.find {
        it.cryptoCurrency == selectedCryptoCurrency && it.fiatCurrency == fiatCurrency
    }?.buyLimits?.minLimit(fiatCurrency)?.valueMinor

    @delegate:Transient
    val isAmountValid: Boolean by unsafeLazy {
        order.amount?.let {
            it <= maxFiatAmount && it >= minFiatAmount
        } ?: false
    }

    @delegate:Transient
    val error: InputError? by unsafeLazy {
        order.amount?.takeIf { it.isZero.not() }?.let {
            when {
                it > maxFiatAmount -> InputError.ABOVE_MAX
                it < minFiatAmount -> InputError.BELOW_MIN
                else -> null
            }
        }
    }
}

enum class KycState {
    /** Docs submitted for Gold and state is pending. Or kyc backend query returned an error  */
    PENDING,

    /** Docs processed, failed kyc. Not error state. */
    FAILED,

    /** Docs processed under manual review */
    IN_REVIEW,

    /** Docs submitted, no result know from server yet */
    UNDECIDED,

    /** Docs uploaded, processed and kyc passed. Eligible for simple buy */
    VERIFIED_AND_ELIGIBLE,

    /** Docs uploaded, processed and kyc passed. User is NOT eligible for simple buy. */
    VERIFIED_BUT_NOT_ELIGIBLE;

    fun verified() = this == VERIFIED_AND_ELIGIBLE || this == VERIFIED_BUT_NOT_ELIGIBLE

    fun docsSubmitted() = this != PENDING
}

enum class FlowScreen {
    INTRO, CURRENCY_SELECTOR, ENTER_AMOUNT, KYC, KYC_VERIFICATION, CHECKOUT, BANK_DETAILS, ADD_CARD
}

enum class InputError {
    BELOW_MIN, ABOVE_MAX
}

sealed class ErrorState {
    object GenericError : ErrorState()
    object NoAvailableCurrenciesToTrade : ErrorState()
}

data class SimpleBuyOrder(
    val orderState: OrderState = OrderState.UNINITIALISED,
    val amount: FiatValue? = null,
    val expirationDate: Date? = null,
    val quote: Quote? = null
)

data class PaymentOptions(
    val availablePaymentMethods: List<PaymentMethod> = emptyList(),
    val canAddCard: Boolean = false,
    val canLinkFunds: Boolean = false
)

data class SelectedPaymentMethod(
    val id: String,
    val partner: Partner? = null,
    val label: String? = "",
    val paymentMethodType: PaymentMethodType
) {
    fun isBank() = paymentMethodType == PaymentMethodType.BANK_ACCOUNT
    fun isCard() = paymentMethodType == PaymentMethodType.PAYMENT_CARD
}