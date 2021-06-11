package piuk.blockchain.android.simplebuy

import com.blockchain.nabu.datamanagers.BuySellPair
import com.blockchain.nabu.datamanagers.CustodialQuote
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.Partner
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.datamanagers.TransferLimits
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.LinkBankTransfer
import com.blockchain.nabu.models.data.LinkedBank
import com.blockchain.nabu.models.data.RecurringBuyFrequency
import com.blockchain.nabu.models.data.RecurringBuyState
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import piuk.blockchain.android.cards.EverypayAuthOptions
import piuk.blockchain.android.ui.base.mvi.MviState
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import java.io.Serializable
import java.math.BigInteger
import java.util.Date

/**
 * This is an object that gets serialized with Gson so any properties that we don't
 * want to get serialized should be tagged as @Transient
 *
 */
data class SimpleBuyState(
    val id: String? = null,
    val supportedPairsAndLimits: List<BuySellPair>? = null,
    private val amount: FiatValue? = null,
    val fiatCurrency: String = "USD",
    val selectedCryptoCurrency: CryptoCurrency? = null,
    val orderState: OrderState = OrderState.UNINITIALISED,
    private val expirationDate: Date? = null,
    val custodialQuote: CustodialQuote? = null,
    val kycStartedButNotCompleted: Boolean = false,
    val kycVerificationState: KycState? = null,
    val currentScreen: FlowScreen = FlowScreen.ENTER_AMOUNT,
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
    @Transient val authorisePaymentUrl: String? = null,
    @Transient val linkedBank: LinkedBank? = null,
    val paymentSucceeded: Boolean = false,
    val showRating: Boolean = false,
    @Transient val shouldShowUnlockHigherFunds: Boolean = false,
    val withdrawalLockPeriod: BigInteger = BigInteger.ZERO,
    @Transient val linkBankTransfer: LinkBankTransfer? = null,
    @Transient val paymentPending: Boolean = false,
    @Transient val transferLimits: TransferLimits? = null,
    // we use this flag to avoid navigating back and forth, reset after navigating
    @Transient val confirmationActionRequested: Boolean = false,
    @Transient val newPaymentMethodToBeAdded: PaymentMethod? = null,
    val recurringBuyFrequency: RecurringBuyFrequency = RecurringBuyFrequency.ONE_TIME,
    val recurringBuyState: RecurringBuyState = RecurringBuyState.UNINITIALISED,
    @Transient private val recurringBuyEligiblePaymentMethods: List<PaymentMethodType> = emptyList()
) : MviState {

    @delegate:Transient
    val order: SimpleBuyOrder by unsafeLazy {
        SimpleBuyOrder(
            orderState,
            amount,
            expirationDate,
            custodialQuote
        )
    }

    @delegate:Transient
    val selectedPaymentMethodDetails: PaymentMethod? by unsafeLazy {
        selectedPaymentMethod?.id?.let { id ->
            paymentOptions.availablePaymentMethods.firstOrNull { it.id == id }
        }
    }

    @delegate:Transient
    val maxFiatAmount: Money by unsafeLazy {
        val maxPaymentMethodLimit = selectedPaymentMethodDetails.maxLimit()
        val maxUserLimit = transferLimits?.maxLimit

        if (maxPaymentMethodLimit != null && maxUserLimit != null)
            Money.min(maxPaymentMethodLimit, maxUserLimit)
        else
            maxPaymentMethodLimit ?: maxUserLimit ?: FiatValue.zero(fiatCurrency)
    }

    @delegate:Transient
    val minFiatAmount: Money by unsafeLazy {
        val minPaymentMethodLimit = selectedPaymentMethodDetails.minLimit()
        val minUserLimit = transferLimits?.minLimit

        if (minPaymentMethodLimit != null && minUserLimit != null)
            Money.max(minPaymentMethodLimit, minUserLimit)
        else
            minPaymentMethodLimit ?: minUserLimit ?: FiatValue.zero(fiatCurrency)
    }

    fun maxCryptoAmount(exchangeRateDataManager: ExchangeRateDataManager): Money? {
        val exchangeRate = ExchangeRate.FiatToCrypto(
            from = fiatCurrency,
            to = selectedCryptoCurrency ?: return null,
            rate = exchangeRateDataManager.getLastPrice(selectedCryptoCurrency, fiatCurrency)

        )
        return exchangeRate.convert(maxFiatAmount)
    }

    fun minCryptoAmount(exchangeRateDataManager: ExchangeRateDataManager): Money? {
        val exchangeRate = ExchangeRate.FiatToCrypto(
            from = fiatCurrency,
            to = selectedCryptoCurrency ?: return null,
            rate = exchangeRateDataManager.getLastPrice(selectedCryptoCurrency, fiatCurrency)

        )
        return exchangeRate.convert(minFiatAmount)
    }

    fun isSelectedPaymentMethodRecurringBuyEligible(): Boolean =
        when (selectedPaymentMethodDetails) {
            is PaymentMethod.Funds -> recurringBuyEligiblePaymentMethods.contains(PaymentMethodType.FUNDS)
            is PaymentMethod.Bank -> recurringBuyEligiblePaymentMethods.contains(PaymentMethodType.BANK_TRANSFER)
            is PaymentMethod.Card -> recurringBuyEligiblePaymentMethods.contains(PaymentMethodType.PAYMENT_CARD)
            else -> false
        }

    private fun PaymentMethod?.maxLimit(): Money? = this?.limits?.max
    private fun PaymentMethod?.minLimit(): Money? = this?.limits?.min

    @delegate:Transient
    val isAmountValid: Boolean by unsafeLazy {
        order.amount?.let {
            it <= maxFiatAmount && it >= minFiatAmount
        } ?: false
    }

    @delegate:Transient
    val error: InputError? by unsafeLazy {
        order.amount?.takeIf { it.isPositive }?.let {
            when {
                it > maxFiatAmount -> InputError.ABOVE_MAX
                it < minFiatAmount -> InputError.BELOW_MIN
                else -> null
            }
        }
    }

    fun shouldLaunchExternalFlow(): Boolean =
        authorisePaymentUrl != null && linkedBank != null && id != null
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
}

enum class FlowScreen {
    ENTER_AMOUNT, KYC, KYC_VERIFICATION, CHECKOUT
}

enum class InputError {
    BELOW_MIN, ABOVE_MAX
}

sealed class ErrorState : Serializable {
    object GenericError : ErrorState()
    object NoAvailableCurrenciesToTrade : ErrorState()
    object BankLinkingUpdateFailed : ErrorState()
    object BankLinkingFailed : ErrorState()
    object BankLinkingTimeout : ErrorState()
    object LinkedBankAlreadyLinked : ErrorState()
    object LinkedBankAccountUnsupported : ErrorState()
    object LinkedBankNamesMismatched : ErrorState()
    object LinkedBankNotSupported : ErrorState()
    object LinkedBankRejected : ErrorState()
    object LinkedBankExpired : ErrorState()
    object LinkedBankFailure : ErrorState()
    object LinkedBankInvalid : ErrorState()
    object ApprovedBankDeclined : ErrorState()
    object ApprovedBankRejected : ErrorState()
    object ApprovedBankFailed : ErrorState()
    object ApprovedBankExpired : ErrorState()
    object ApprovedGenericError : ErrorState()
    object DailyLimitExceeded : ErrorState()
    object WeeklyLimitExceeded : ErrorState()
    object YearlyLimitExceeded : ErrorState()
    object ExistingPendingOrder : ErrorState()
}

data class SimpleBuyOrder(
    val orderState: OrderState = OrderState.UNINITIALISED,
    val amount: FiatValue? = null,
    val expirationDate: Date? = null,
    val custodialQuote: CustodialQuote? = null
)

data class PaymentOptions(
    val availablePaymentMethods: List<PaymentMethod> = emptyList(),
    val canAddCard: Boolean = false,
    val canLinkFunds: Boolean = false,
    val canLinkBank: Boolean = false
)

data class SelectedPaymentMethod(
    val id: String,
    val partner: Partner? = null,
    val label: String? = "",
    val paymentMethodType: PaymentMethodType,
    val isEligible: Boolean
) {
    fun isCard() = paymentMethodType == PaymentMethodType.PAYMENT_CARD
    fun isBank() = paymentMethodType == PaymentMethodType.BANK_TRANSFER
    fun isFunds() = paymentMethodType == PaymentMethodType.FUNDS

    fun concreteId(): String? =
        if (isDefinedBank() || isDefinedCard()) id else null

    private fun isDefinedCard() = paymentMethodType == PaymentMethodType.PAYMENT_CARD &&
        id != PaymentMethod.UNDEFINED_CARD_PAYMENT_ID

    private fun isDefinedBank() = paymentMethodType == PaymentMethodType.BANK_TRANSFER &&
        id != PaymentMethod.UNDEFINED_BANK_TRANSFER_PAYMENT_ID

    fun isActive() =
        concreteId() != null || (paymentMethodType == PaymentMethodType.FUNDS && id == PaymentMethod.FUNDS_PAYMENT_ID)
}