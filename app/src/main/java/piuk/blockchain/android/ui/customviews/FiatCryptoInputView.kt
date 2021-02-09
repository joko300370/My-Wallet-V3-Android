package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.ExchangeRates
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.balance.hasOppositeSourceAndTarget
import info.blockchain.balance.hasSameSourceAndTarget
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.enter_fiat_crypto_layout.view.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.customviews.inputview.DecimalDigitsInputFilter
import piuk.blockchain.android.util.afterMeasured
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import piuk.blockchain.android.util.visibleIf
import piuk.blockchain.android.util.AfterTextChangedWatcher
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale
import kotlin.properties.Delegates

class FiatCryptoInputView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs), KoinComponent {

    val onImeAction: Observable<PrefixedOrSuffixedEditText.ImeOptions> by lazy {
        enter_amount.onImeAction
    }

    private val amountSubject: PublishSubject<Money> = PublishSubject.create()

    private val inputToggleSubject: PublishSubject<CurrencyType> = PublishSubject.create()

    val onInputToggle: Observable<CurrencyType>
        get() = inputToggleSubject

    val amount: Observable<Money>
        get() = amountSubject

    private val defExchangeRates: ExchangeRates by scopedInject()

    private val currencyPrefs: CurrencyPrefs by inject()

    var customInternalExchangeRate: ExchangeRate? = null
        set(value) {
            field = value
            updateExchangeAmountAndOutput()
        }

    private val inputToOutputExchangeRate: ExchangeRate
        get() = configuration.defInputToOutputExchangeRate()

    private val internalExchangeRate: ExchangeRate
        get() {
            val defInternalExchangeRate = configuration.defInternalExchangeRate()
            return customInternalExchangeRate?.let {
                check(
                    it.hasSameSourceAndTarget(defInternalExchangeRate) ||
                        it.hasOppositeSourceAndTarget(defInternalExchangeRate)
                ) {
                    "Custom exchange rate provided is not supported." +
                        "Should be from ${configuration.inputCurrency} to ${configuration.exchangeCurrency} " +
                        "or vice versa"
                }
                return if (defInternalExchangeRate.hasSameSourceAndTarget(it)) it else it.inverse(RoundingMode.CEILING)
            } ?: defInternalExchangeRate
        }

    private val compositeDisposable = CompositeDisposable()

    init {
        inflate(context, R.layout.enter_fiat_crypto_layout, this)

        compositeDisposable += enter_amount.textSize.subscribe { textSize ->
            if (enter_amount.text.toString() == enter_amount.configuration.prefixOrSuffix) {
                placeFakeHint(textSize, enter_amount.configuration.isPrefix)
            } else
                fake_hint.gone()
        }

        enter_amount.addTextChangedListener(object : AfterTextChangedWatcher() {
            override fun afterTextChanged(s: Editable?) {
                updateExchangeAmountAndOutput()
            }
        })

        currency_swap.setOnClickListener {
            configuration =
                configuration.copy(
                    inputCurrency = configuration.exchangeCurrency,
                    outputCurrency = configuration.exchangeCurrency,
                    exchangeCurrency = configuration.inputCurrency,
                    predefinedAmount = internalExchangeRate.convert(getLastEnteredAmount(configuration))
                )
            inputToggleSubject.onNext(configuration.inputCurrency)
        }
    }

    var configured = false
        private set

    private fun placeFakeHint(textSize: Int, hasPrefix: Boolean) {
        fake_hint.visible()
        fake_hint.afterMeasured {
            it.translationX =
                if (hasPrefix) (enter_amount.width / 2f + textSize / 2f) +
                    resources.getDimensionPixelOffset(R.dimen.smallest_margin) else
                    enter_amount.width / 2f - textSize / 2f - it.width -
                        resources.getDimensionPixelOffset(R.dimen.smallest_margin)
        }
    }

    private fun getLastEnteredAmount(configuration: FiatCryptoViewConfiguration): Money =
        enter_amount.bigDecimalValue?.let { enterAmount ->
            when (configuration.inputCurrency) {
                is CurrencyType.Fiat -> FiatValue.fromMajor(configuration.inputCurrency.fiatCurrency, enterAmount)
                is CurrencyType.Crypto -> CryptoValue.fromMajor(configuration.inputCurrency.cryptoCurrency, enterAmount)
            }
        } ?: configuration.inputCurrency.zeroValue()

    var configuration: FiatCryptoViewConfiguration by Delegates.observable(
        FiatCryptoViewConfiguration(
            inputCurrency = CurrencyType.Fiat(currencyPrefs.selectedFiatCurrency),
            outputCurrency = CurrencyType.Fiat(currencyPrefs.selectedFiatCurrency),
            exchangeCurrency = CurrencyType.Fiat(currencyPrefs.selectedFiatCurrency)
        )
    ) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            configured = true
            enter_amount.filters = emptyArray()

            val inputSymbol = newValue.inputCurrency.symbol()

            currency_swap.visibleIf { newValue.swapEnabled }
            exchange_amount.visibleIf { newValue.inputCurrency != newValue.exchangeCurrency }

            maxLimit?.let { updateFilters(inputSymbol, it.toInputCurrency()) }
            fake_hint.text = newValue.inputCurrency.zeroValue().toStringWithoutSymbol()

            enter_amount.configuration = Configuration(
                prefixOrSuffix = inputSymbol,
                isPrefix = newValue.inputCurrency is CurrencyType.Fiat,
                initialText = newValue.predefinedAmount.toStringWithoutSymbol()
                    .replace(DecimalFormatSymbols(Locale.getDefault()).groupingSeparator.toString(), "")
                    .removeSuffix("${DecimalFormatSymbols(Locale.getDefault()).decimalSeparator}00")
            )

            enter_amount.resetForTyping()
        }
    }

    var maxLimit by Delegates.observable<Money?>(null) { _, oldValue, newValue ->
        if (newValue != oldValue && newValue != null)
            updateFilters(enter_amount.configuration.prefixOrSuffix, newValue.toInputCurrency())
    }

    fun showError(errorMessage: String, shouldDisableInput: Boolean = false) {
        error.text = errorMessage
        error.visible()
        info.gone()
        hideExchangeAmount()
        exchange_amount.isEnabled = !shouldDisableInput
    }

    fun showInfo(infoMessage: String, onClick: () -> Unit) {
        info.text = infoMessage
        error.gone()
        info.visible()
        info.setOnClickListener {
            onClick()
        }
        hideExchangeAmount()
    }

    private fun hideExchangeAmount() {
        exchange_amount.gone()
    }

    fun hideLabels() {
        error.gone()
        info.gone()
        showExchangeAmount()
    }

    private fun showExchangeAmount() {
        exchange_amount.visible()
    }

    private fun showValue(money: Money) {
        configuration = configuration.copy(
            predefinedAmount = money
        )
    }

    private fun updateFilters(prefixOrSuffix: String, value: Money) {
        val maxDecimalDigitsForAmount = value.userDecimalPlaces
        val maxIntegerDigitsForAmount = value.toStringParts().major.length
        enter_amount.addFilter(maxDecimalDigitsForAmount, maxIntegerDigitsForAmount, prefixOrSuffix)
    }

    private fun PrefixedOrSuffixedEditText.addFilter(
        maxDecimalDigitsForAmount: Int,
        maxIntegerDigitsForAmount: Int,
        prefixOrSuffix: String
    ) {
        filters =
            arrayOf(
                DecimalDigitsInputFilter(
                    digitsAfterZero = maxDecimalDigitsForAmount,
                    prefixOrSuffix = prefixOrSuffix
                )
            )
    }

    private fun updateExchangeAmountAndOutput() {
        val config = configuration.inputCurrency

        if (config is CurrencyType.Fiat) {
            val enteredAmount = enter_amount.bigDecimalValue?.let { amount ->
                FiatValue.fromMajor(config.fiatCurrency, amount)
            } ?: FiatValue.zero(config.fiatCurrency)

            val output = inputToOutputExchangeRate.convert(enteredAmount)
            val internalExchangeAmount = internalExchangeRate.convert(enteredAmount)
            exchange_amount.text = internalExchangeAmount.toStringWithSymbol()
            amountSubject.onNext(output)
        } else if (config is CurrencyType.Crypto) {

            val cryptoAmount = enter_amount.bigDecimalValue?.let { amount ->
                CryptoValue.fromMajor(config.cryptoCurrency, amount)
            } ?: CryptoValue.zero(config.cryptoCurrency)

            val output = inputToOutputExchangeRate.convert(cryptoAmount)
            val internalExchangeAmount = internalExchangeRate.convert(cryptoAmount)

            exchange_amount.text = internalExchangeAmount.toStringWithSymbol()
            amountSubject.onNext(output)
        }
    }

    fun fixExchange(it: Money) {
        exchange_amount.text = it.toStringWithSymbol()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        compositeDisposable.clear()
    }

    private fun FiatCryptoViewConfiguration.defInternalExchangeRate() =
        exchangeRate(inputCurrency, exchangeCurrency)

    private fun FiatCryptoViewConfiguration.defInputToOutputExchangeRate() =
        exchangeRate(inputCurrency, outputCurrency)

    private fun exchangeRate(
        input: CurrencyType,
        output: CurrencyType
    ): ExchangeRate {
        return when (input) {
            is CurrencyType.Fiat -> when (output) {
                is CurrencyType.Crypto -> {
                    ExchangeRate.CryptoToFiat(
                        to = input.fiatCurrency,
                        from = output.cryptoCurrency,
                        _rate = defExchangeRates.getLastPrice(output.cryptoCurrency, input.fiatCurrency)
                    ).inverse(RoundingMode.CEILING, output.cryptoCurrency.userDp)
                }
                is CurrencyType.Fiat -> {
                    ExchangeRate.FiatToFiat(
                        input.fiatCurrency,
                        output.fiatCurrency,
                        defExchangeRates.getLastPriceOfFiat(output.fiatCurrency, input.fiatCurrency)
                    )
                }
            }
            is CurrencyType.Crypto -> when (output) {
                is CurrencyType.Crypto -> {
                    ExchangeRate.CryptoToCrypto(
                        from = input.cryptoCurrency,
                        to = output.cryptoCurrency,
                        rate = if (output.cryptoCurrency != input.cryptoCurrency) throw NotImplementedError(
                            ""
                        ) else 1.toBigDecimal()
                    )
                }
                is CurrencyType.Fiat -> {
                    ExchangeRate.CryptoToFiat(
                        from = input.cryptoCurrency,
                        to = output.fiatCurrency,
                        _rate = defExchangeRates.getLastPrice(input.cryptoCurrency, output.fiatCurrency)
                    )
                }
            }
        }
    }

    fun updateValue(amount: Money) {
        if (configuration.inputCurrency is CurrencyType.Fiat && amount is CryptoValue) {
            configuration = configuration.copy(
                inputCurrency = CurrencyType.Crypto(amount.currency),
                exchangeCurrency = configuration.inputCurrency,
                outputCurrency = CurrencyType.Crypto(amount.currency)
            )
        }
        showValue(amount)
    }

    private fun Money.toInputCurrency(): Money {
        val input = configuration.inputCurrency

        val currency = when (this) {
            is FiatValue -> CurrencyType.Fiat(this.currencyCode)
            is CryptoValue -> CurrencyType.Crypto(this.currency)
            else -> throw IllegalStateException("Not supported currency")
        }

        when (currency) {
            input -> {
                return this
            }
            configuration.outputCurrency -> {
                return inputToOutputExchangeRate.inverse().convert(this)
            }
            configuration.exchangeCurrency -> {
                return internalExchangeRate.inverse().convert(this)
            }
            else -> {
                throw IllegalStateException(
                    "Provided amount should be in one of the following ${input.rawCurrency()} " +
                        "or ${configuration.outputCurrency.rawCurrency()} " +
                        "or ${configuration.exchangeCurrency}"
                )
            }
        }
    }
}

data class FiatCryptoViewConfiguration(
    val inputCurrency: CurrencyType, // the currency used for input by the user
    val exchangeCurrency: CurrencyType, // the currency used for the exchanged amount
    val outputCurrency: CurrencyType = inputCurrency, // the currency used for the model output
    val predefinedAmount: Money = inputCurrency.zeroValue(),
    val canSwap: Boolean = true
) {

    val swapEnabled: Boolean
        get() = canSwap && inputCurrency != exchangeCurrency
}

private fun CurrencyType.zeroValue(): Money =
    when (this) {
        is CurrencyType.Fiat -> FiatValue.zero(fiatCurrency)
        is CurrencyType.Crypto -> CryptoValue.zero(cryptoCurrency)
    }

private fun CurrencyType.symbol(): String =
    when (this) {
        is CurrencyType.Fiat -> Currency.getInstance(fiatCurrency).getSymbol(Locale.getDefault())
        is CurrencyType.Crypto -> cryptoCurrency.displayTicker
    }

private fun CurrencyType.rawCurrency(): String =
    when (this) {
        is CurrencyType.Fiat -> fiatCurrency
        is CurrencyType.Crypto -> cryptoCurrency.displayTicker
    }

sealed class CurrencyType {
    data class Fiat(val fiatCurrency: String) : CurrencyType()
    data class Crypto(val cryptoCurrency: CryptoCurrency) : CurrencyType()

    fun isCrypto() = this is Crypto
    fun isFiat() = this is Fiat

    fun isSameType(money: Money) =
        when (this) {
            is Fiat -> money is FiatValue
            is Crypto -> money is CryptoValue
        }
}