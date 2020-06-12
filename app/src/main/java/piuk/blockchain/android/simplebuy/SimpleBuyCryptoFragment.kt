package piuk.blockchain.android.simplebuy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import com.blockchain.extensions.exhaustive
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.CurrencyChangedFromBuyForm
import com.blockchain.notifications.analytics.PaymentMethodSelected
import com.blockchain.notifications.analytics.SimpleBuyAnalytics
import com.blockchain.notifications.analytics.buyConfirmClicked
import com.blockchain.notifications.analytics.cryptoChanged
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.PaymentMethod
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_simple_buy_buy_crypto.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.cards.CardDetailsActivity
import piuk.blockchain.android.cards.CardDetailsActivity.Companion.ADD_CARD_REQUEST_CODE
import piuk.blockchain.android.cards.icon
import piuk.blockchain.android.ui.base.ErrorSlidingBottomDialog
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.android.ui.customviews.CurrencyType
import piuk.blockchain.android.ui.customviews.FiatCryptoViewConfiguration
import piuk.blockchain.android.util.assetName
import piuk.blockchain.android.util.drawableResFilled
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.extensions.visibleIf
import java.text.DecimalFormatSymbols
import java.util.Locale

class SimpleBuyCryptoFragment : MviFragment<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState>(),
    SimpleBuyScreen,
    PaymentMethodChangeListener,
    ChangeCurrencyHost {

    override val model: SimpleBuyModel by scopedInject()
    private val exchangeRateDataManager: ExchangeRateDataManager by scopedInject()

    private var lastState: SimpleBuyState? = null
    private val compositeDesposable = CompositeDisposable()

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator)
            ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    private val currencyPrefs: CurrencyPrefs by inject()

    override fun onBackPressed(): Boolean = true
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_simple_buy_buy_crypto)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.setupToolbar(R.string.simple_buy_buy_crypto_title)
        model.process(SimpleBuyIntent.SyncState)
        model.process(SimpleBuyIntent.FetchBuyLimits(currencyPrefs.selectedFiatCurrency))
        model.process(SimpleBuyIntent.FlowCurrentScreen(FlowScreen.ENTER_AMOUNT))
        model.process(SimpleBuyIntent.FetchPredefinedAmounts(currencyPrefs.selectedFiatCurrency))
        model.process(SimpleBuyIntent.FetchSuggestedPaymentMethod(currencyPrefs.selectedFiatCurrency))
        model.process(SimpleBuyIntent.FetchSupportedFiatCurrencies)
        analytics.logEvent(SimpleBuyAnalytics.BUY_FORM_SHOWN)

        compositeDesposable += input_amount.amount.subscribe {
            when (it) {
                is FiatValue -> model.process(SimpleBuyIntent.AmountUpdated(it))
                else -> throw IllegalStateException("CryptoValue is not supported as input yet")
            }
        }

        btn_continue.setOnClickListener {
            model.process(SimpleBuyIntent.BuyButtonClicked)
            analytics.logEvent(buyConfirmClicked(
                lastState?.order?.amount?.valueMinor.toString(),
                lastState?.fiatCurrency ?: "")
            )
        }

        payment_method_root.setOnClickListener {
            lastState?.paymentOptions?.let {
                showBottomSheet(PaymentMethodChooserBottomSheet.newInstance(it.availablePaymentMethods.filterNot {
                    it is PaymentMethod.Undefined
                },
                    it.canAddCard))
            }
        }
        fiat_currency.setOnClickListener {
            showBottomSheet(
                FiatCurrencyChooserBottomSheet
                    .newInstance(lastState?.supportedFiatCurrencies ?: return@setOnClickListener)
            )
        }
    }

    override fun onFiatCurrencyChanged(fiatCurrency: String) {
        if (fiatCurrency == lastState?.fiatCurrency) return
        model.process(SimpleBuyIntent.FiatCurrencyUpdated(fiatCurrency))
        model.process(SimpleBuyIntent.FetchBuyLimits(fiatCurrency))
        model.process(SimpleBuyIntent.FetchPredefinedAmounts(fiatCurrency))
        model.process(SimpleBuyIntent.FetchSuggestedPaymentMethod(currencyPrefs.selectedFiatCurrency))
        analytics.logEvent(CurrencyChangedFromBuyForm(fiatCurrency))
    }

    override fun onCryptoCurrencyChanged(currency: CryptoCurrency) {
        model.process(SimpleBuyIntent.NewCryptoCurrencySelected(currency))
        analytics.logEvent(cryptoChanged(currency))
        input_amount.configuration = input_amount.configuration.copy(
            cryptoCurrency = currency,
            predefinedAmount = CryptoValue.zero(currency)
        )
    }

    override fun render(newState: SimpleBuyState) {
        lastState = newState

        if (newState.errorState != null) {
            showErrorState(newState.errorState)
            return
        }
        newState.selectedCryptoCurrency?.let {
            if (!input_amount.isConfigured) {
                input_amount.configuration = FiatCryptoViewConfiguration(
                    input = CurrencyType.Fiat,
                    output = CurrencyType.Fiat,
                    fiatCurrency = newState.fiatCurrency,
                    cryptoCurrency = it,
                    predefinedAmount = newState.order.amount ?: FiatValue.zero(newState.fiatCurrency)
                )
            }
        }
        fiat_currency.text = newState.fiatCurrency
        newState.selectedCryptoCurrency?.let {
            crypto_icon.setImageResource(it.drawableResFilled())
            crypto_text.setText(it.assetName())
        }

        newState.exchangePrice?.let {
            crypto_exchange_rate.text =
                "1 ${newState.selectedCryptoCurrency?.displayTicker} = ${it.toStringWithSymbol()}"
        }

        arrow.visibleIf { newState.availableCryptoCurrencies.size > 1 }

        input_amount.maxLimit = newState.maxFiatAmount

        newState.predefinedAmounts.takeIf {
            it.isNotEmpty() && newState.selectedCryptoCurrency != null
        }?.let { values ->
            predefined_amount_1.asPredefinedAmountText(values.getOrNull(0))
            predefined_amount_2.asPredefinedAmountText(values.getOrNull(1))
            predefined_amount_3.asPredefinedAmountText(values.getOrNull(2))
            predefined_amount_4.asPredefinedAmountText(values.getOrNull(3))
        } ?: kotlin.run {
            hidePredefinedAmounts()
        }

        newState.selectedPaymentMethodDetails?.let {
            renderPaymentMethod(it)
        } ?: payment_method_root.gone()

        btn_continue.isEnabled = canContinue(newState)
        newState.error?.let {
            handleError(it, newState)
        } ?: kotlin.run {
            clearError()
        }

        coin_selector.takeIf { newState.availableCryptoCurrencies.size > 1 }?.setOnClickListener {
            showBottomSheet(
                CryptoCurrencyChooserBottomSheet
                    .newInstance(newState.availableCryptoCurrencies)
            )
        }

        if (newState.confirmationActionRequested && newState.kycVerificationState != null) {
            when (newState.kycVerificationState) {
                // Kyc state unknown because error, or gold docs unsubmitted
                KycState.PENDING -> {
                    model.process(SimpleBuyIntent.ConfirmationHandled)
                    model.process(SimpleBuyIntent.KycStarted)
                    navigator().startKyc()
                    analytics.logEvent(SimpleBuyAnalytics.START_GOLD_FLOW)
                }
                // Awaiting results state
                KycState.IN_REVIEW,
                KycState.UNDECIDED -> {
                    navigator().goToKycVerificationScreen()
                }
                // Got results, kyc verification screen will show error
                KycState.VERIFIED_BUT_NOT_ELIGIBLE,
                KycState.FAILED -> {
                    navigator().goToKycVerificationScreen()
                }
                // We have done kyc and are verified
                KycState.VERIFIED_AND_ELIGIBLE -> {
                    navigator().goToCheckOutScreen()
                }
            }.exhaustive
        }
    }

    private fun canContinue(state: SimpleBuyState) =
        state.isAmountValid && state.selectedPaymentMethod != null

    private fun renderPaymentMethod(selectedPaymentMethod: PaymentMethod) {

        when (selectedPaymentMethod) {
            is PaymentMethod.Undefined -> {
                payment_method_icon.setImageResource(R.drawable.ic_add_payment_method)
            }
            is PaymentMethod.BankTransfer -> renderBankPayment(selectedPaymentMethod)
            is PaymentMethod.Card -> renderCardPayment(selectedPaymentMethod)
            is PaymentMethod.UndefinedCard -> renderUndefinedCardPayment(selectedPaymentMethod)
        }
        payment_method_root.visible()
        undefined_payment_text.visibleIf { selectedPaymentMethod is PaymentMethod.Undefined }
        payment_method_title.visibleIf { (selectedPaymentMethod is PaymentMethod.Undefined).not() }
        payment_method_limit.visibleIf { (selectedPaymentMethod is PaymentMethod.Undefined).not() }
    }

    private fun renderUndefinedCardPayment(selectedPaymentMethod: PaymentMethod.UndefinedCard) {
        payment_method_icon.setImageResource(R.drawable.ic_payment_card)
        payment_method_title.text = getString(R.string.credit_or_debit_card)
        payment_method_limit.text =
            getString(R.string.payment_method_limit, selectedPaymentMethod.limits.max.toStringWithSymbol())
    }

    private fun renderCardPayment(selectedPaymentMethod: PaymentMethod.Card) {
        payment_method_icon.setImageResource(selectedPaymentMethod.cardType.icon())
        payment_method_title.text = selectedPaymentMethod.uiLabelWithDigits()
        payment_method_limit.text =
            getString(R.string.payment_method_limit, selectedPaymentMethod.limits.max.toStringWithSymbol())
    }

    private fun renderBankPayment(selectedPaymentMethod: PaymentMethod.BankTransfer) {
        payment_method_title.text = getString(R.string.bank_wire_transfer)
        payment_method_icon.setImageResource(R.drawable.ic_bank_transfer)
        payment_method_limit.text =
            getString(R.string.payment_method_limit, selectedPaymentMethod.limits.max.toStringWithSymbol())
    }

    private fun clearError() {
        input_amount.hideError()
    }

    private fun showErrorState(errorState: ErrorState) {
        showBottomSheet(ErrorSlidingBottomDialog.newInstance(activity))
    }

    private fun handleError(error: InputError, state: SimpleBuyState) {
        when (error) {
            InputError.ABOVE_MAX -> {
                input_amount.showError(
                    if (input_amount.configuration?.input == CurrencyType.Fiat)
                        resources.getString(R.string.maximum_buy, state.maxFiatAmount?.toStringWithSymbol())
                    else
                        resources.getString(R.string.maximum_buy,
                            state.maxCryptoAmount(exchangeRateDataManager)?.toStringWithSymbol())
                )
            }
            InputError.BELOW_MIN -> {
                input_amount.showError(
                    if (input_amount.configuration?.input == CurrencyType.Fiat)
                        resources.getString(R.string.minimum_buy, state.minFiatAmount?.toStringWithSymbol())
                    else
                        resources.getString(R.string.minimum_buy,
                            state.minCryptoAmount(exchangeRateDataManager)?.toStringWithSymbol())
                )
            }
        }
    }

    private fun hidePredefinedAmounts() {
        predefined_amount_1.gone()
        predefined_amount_2.gone()
        predefined_amount_3.gone()
        predefined_amount_4.gone()
    }

    private fun FiatValue.asInputAmount(): String =
        this.toStringWithoutSymbol().withoutThousandsSeparator().withoutTrailingDecimalsZeros()

    private fun AppCompatTextView.asPredefinedAmountText(amount: FiatValue?) {
        amount?.let { amnt ->
            text = amnt.formatOrSymbolForZero().withoutTrailingDecimalsZeros()
            visible()
        } ?: this.gone()
    }

    private fun String.withoutThousandsSeparator(): String =
        replace(DecimalFormatSymbols(Locale.getDefault()).groupingSeparator.toString(), "")

    private fun String.withoutTrailingDecimalsZeros(): String =
        replace("${DecimalFormatSymbols(Locale.getDefault()).decimalSeparator}00", "")

    override fun onPause() {
        super.onPause()
        model.process(SimpleBuyIntent.ConfirmationHandled)
    }

    override fun onSheetClosed() {
        model.process(SimpleBuyIntent.ClearError)
    }

    override fun onPaymentMethodChanged(paymentMethod: PaymentMethod) {
        model.process(SimpleBuyIntent.SelectedPaymentMethodUpdate(paymentMethod))
        analytics.logEvent(PaymentMethodSelected(
            if (paymentMethod is PaymentMethod.BankTransfer) BANK_ANALYTICS
            else CARD_ANALYTICS
        ))
    }

    override fun addPaymentMethod() {
        val intent = Intent(activity, CardDetailsActivity::class.java)
        startActivityForResult(intent, ADD_CARD_REQUEST_CODE)
        analytics.logEvent(PaymentMethodSelected(NEW_CARD_ANALYTICS))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_CARD_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            model.process(
                SimpleBuyIntent.FetchSuggestedPaymentMethod(currencyPrefs.selectedFiatCurrency,
                    (data?.extras?.getSerializable(CardDetailsActivity.CARD_KEY) as? PaymentMethod.Card)?.id
                ))
        }
    }

    companion object {
        private const val BANK_ANALYTICS = "BANK"
        private const val CARD_ANALYTICS = "CARD"
        private const val NEW_CARD_ANALYTICS = "CARD"
    }
}

interface PaymentMethodChangeListener {
    fun onPaymentMethodChanged(paymentMethod: PaymentMethod)
    fun addPaymentMethod()
}

interface ChangeCurrencyHost : SimpleBuyScreen {
    fun onFiatCurrencyChanged(fiatCurrency: String)
    fun onCryptoCurrencyChanged(currency: CryptoCurrency)
}