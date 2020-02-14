package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import com.blockchain.notifications.analytics.SimpleBuyAnalytics
import com.blockchain.notifications.analytics.cryptoChanged
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import kotlinx.android.synthetic.main.fragment_simple_buy_buy_crypto.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.ErrorSlidingBottomDialog
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.android.util.drawableResFilled
import piuk.blockchain.androidcoreui.utils.DecimalDigitsInputFilter
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.extensions.visibleIf
import piuk.blockchain.androidcoreui.utils.helperfunctions.AfterTextChangedWatcher
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.Currency

class SimpleBuyCryptoFragment : MviFragment<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState>(),
    SimpleBuyScreen,
    CurrencyChangeListener {

    override val model: SimpleBuyModel by inject()

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator) ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    private val currencyPrefs: CurrencyPrefs by inject()

    private val fiatSymbol: String
        get() = Currency.getInstance(currencyPrefs.selectedFiatCurrency).getSymbol(Locale.getDefault())

    override fun onBackPressed(): Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_simple_buy_buy_crypto)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.setupToolbar(R.string.simple_buy_buy_crypto_title)
        model.process(SimpleBuyIntent.FetchBuyLimits(currencyPrefs.selectedFiatCurrency))
        model.process(SimpleBuyIntent.FlowCurrentScreen(FlowScreen.ENTER_AMOUNT))
        model.process(SimpleBuyIntent.FetchPredefinedAmounts(currencyPrefs.selectedFiatCurrency))
        analytics.logEvent(SimpleBuyAnalytics.BUY_FORM_SHOWN)
        fiat_currency_symbol.text = fiatSymbol
        input_layout_amount.isHintAnimationEnabled = false
        input_amount.addTextChangedListener(object : AfterTextChangedWatcher() {
            override fun afterTextChanged(s: Editable?) {
                input_layout_amount.hint =
                    if (s?.toString().isNullOrEmpty() && input_amount.hasFocus().not()) "0.00" else ""
                model.process(SimpleBuyIntent.EnteredAmount(s.toString()))
            }
        })

        input_amount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                input_layout_amount.hint = ""
            }
        }

        btn_continue.setOnClickListener {
            model.process(SimpleBuyIntent.BuyButtonClicked)
            analytics.logEvent(SimpleBuyAnalytics.BUY_CONFIRM_CLICKED)
        }
    }

    override fun onCurrencyChanged(currency: CryptoCurrency) {

        model.process(SimpleBuyIntent.NewCryptoCurrencySelected(currency))
        input_amount.clearFocus()
        analytics.logEvent(cryptoChanged(currency.symbol))
    }

    override fun render(newState: SimpleBuyState) {
        if (newState.errorState != null) {
            showErrorState(newState.errorState)
            return
        }

        newState.selectedCryptoCurrency?.let {
            crypto_icon.setImageResource(it.drawableResFilled())
            crypto_text.text = it.unit
            activity.setupToolbar(resources.getString(R.string.simple_buy_token, it.symbol))
        }
        arrow.visibleIf { newState.availableCryptoCurrencies.size > 1 }
        if (newState.maxAmount != null && newState.minAmount != null) {
            input_amount.filters =
                arrayOf(DecimalDigitsInputFilter(newState.maxIntegerDigitsForAmount(),
                    newState.maxDecimalDigitsForAmount()))
            up_to_amount.visible()
            up_to_amount.text =
                getString(R.string.simple_buy_up_to_amount, newState.maxAmount!!.formatOrSymbolForZero())
        }

        newState.predefinedAmounts.takeIf {
            it.isNotEmpty() && newState.selectedCryptoCurrency != null
        }?.let { values ->
            predefined_amount_1.asPredefinedAmountText(values.getOrNull(0))
            predefined_amount_2.asPredefinedAmountText(values.getOrNull(1))
            predefined_amount_3.asPredefinedAmountText(values.getOrNull(2))
            predefined_amount_4.asPredefinedAmountText(values.getOrNull(3))
        } ?: kotlin.run {
            predefined_amount_1.gone()
            predefined_amount_2.gone()
            predefined_amount_3.gone()
            predefined_amount_4.gone()
        }

        btn_continue.isEnabled = newState.isAmountValid
        input_amount.isEnabled = newState.selectedCryptoCurrency != null
        error_icon.goneIf(newState.error == null)
        input_layout_amount.error = if (newState.error != null) " " else null

        newState.error?.let {
            handleError(it, newState)
        } ?: kotlin.run {
            error_action.gone()
        }

        if (input_amount.text.toString() != newState.enteredAmount) {
            input_amount.setText(newState.enteredAmount)
        }

        crypto_text.takeIf { newState.availableCryptoCurrencies.size > 1 }?.setOnClickListener {
            showBottomSheet(CryptoCurrencyChooserBottomSheet
                .newInstance(newState.availableCryptoCurrencies))
        }

        if (newState.confirmationActionRequested && newState.kycVerificationState != null) {
            when {
                newState.kycVerificationState.verified().not() -> {
                    model.process(SimpleBuyIntent.ConfirmationHandled)
                    model.process(SimpleBuyIntent.KycStarted)
                    navigator().startKyc()
                    analytics.logEvent(SimpleBuyAnalytics.START_GOLD_FLOW)
                }
                newState.kycVerificationState.kycDataAlreadySubmitted() -> {
                    navigator().goToKycVerificationScreen()
                }
                else -> {
                    navigator().goToCheckOutScreen()
                }
            }
        }
    }

    private fun showErrorState(errorState: ErrorState) {
        showBottomSheet(ErrorSlidingBottomDialog.newInstance(activity))
    }

    private fun handleError(error: InputError, state: SimpleBuyState) {
        when (error) {
            InputError.ABOVE_MAX -> {
                error_action.apply {
                    text = resources.getString(R.string.use_max)
                    visible()
                    setOnClickListener {
                        input_amount.setText(state.maxAmount?.asInputAmount() ?: "")
                        analytics.logEvent(SimpleBuyAnalytics.BUY_MAX_CLICKED)
                    }
                }
                up_to_amount.text = resources.getString(R.string.too_high)
            }
            InputError.BELOW_MIN -> {
                error_action.apply {
                    text = resources.getString(R.string.use_min)
                    visible()
                    setOnClickListener {
                        analytics.logEvent(SimpleBuyAnalytics.BUY_MIN_CLICKED)
                        input_amount.setText(state.minAmount?.asInputAmount() ?: "")
                    }
                }
                up_to_amount.text = resources.getString(R.string.too_low)
            }
        }
    }

    private fun FiatValue.asInputAmount(): String =
        this.toStringWithoutSymbol().withoutThousandsSeparator().withoutTrailingDecimalsZeros()

    private fun AppCompatTextView.asPredefinedAmountText(amount: FiatValue?) {
        amount?.let { amount ->
            text = amount.formatOrSymbolForZero().withoutTrailingDecimalsZeros()
            visible()
            setOnClickListener {
                input_amount.setText(amount.asInputAmount())
            }
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
}

interface CurrencyChangeListener {
    fun onCurrencyChanged(currency: CryptoCurrency)
}