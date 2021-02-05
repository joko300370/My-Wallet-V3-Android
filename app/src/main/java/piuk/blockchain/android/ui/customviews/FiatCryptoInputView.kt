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
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.enter_fiat_crypto_layout.view.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import piuk.blockchain.android.R
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.android.ui.customviews.inputview.DecimalDigitsInputFilter
import piuk.blockchain.android.util.afterMeasured
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import piuk.blockchain.android.util.visibleIf
import piuk.blockchain.android.util.AfterTextChangedWatcher
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.Currency
import kotlin.properties.Delegates

class FiatCryptoInputView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs), KoinComponent {

    val onImeAction: Observable<PrefixedOrSuffixedEditText.ImeOptions> by lazy {
        enter_amount.onImeAction
    }

    private val amountSubject: PublishSubject<Money> = PublishSubject.create()

    private val inputToggleSubject: PublishSubject<Either<String, CryptoCurrency>> = PublishSubject.create()

    val onInputToggle: Observable<Either<String, CryptoCurrency>>
        get() = inputToggleSubject

    val amount: Observable<Money>
        get() = amountSubject

    private val defExchangeRateDataManager: ExchangeRateDataManager by scopedInject()

    private val defCryptoToFiat: ExchangeRate
        get() = configuration.defExchgangeRate()

    private val currencyPrefs: CurrencyPrefs by inject()

    var exchangeRate: ExchangeRate? = null
        set(value) {
            field = value
            //  updateExchangeAmountAndOutput()
        }

    private val inputToOutputExchangeRate: ExchangeRate
        get() = defCryptoToFiat

    private val internalExchangeRate: ExchangeRate
        get() = configuration.internalExchgangeRate()

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
                is Either.Left -> FiatValue.fromMajor(configuration.inputCurrency.left, enterAmount)
                is Either.Right -> CryptoValue.fromMajor(configuration.inputCurrency.right, enterAmount)
            }
        } ?: configuration.inputCurrency.zeroValue()

    var configuration: FiatCryptoViewConfiguration by Delegates.observable(
        FiatCryptoViewConfiguration(
            inputCurrency = Either.Left(currencyPrefs.selectedFiatCurrency),
            outputCurrency = Either.Left(currencyPrefs.selectedFiatCurrency),
            exchangeCurrency = Either.Left(currencyPrefs.selectedFiatCurrency)
        )
    ) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            configured = true
            enter_amount.filters = emptyArray()

            val inputSymbol = newValue.inputCurrency.symbol()

            currency_swap.visibleIf { newValue.swapEnabled }

            if (newValue.inputCurrency is Either.Left) {
            //    updateFilters(fiatSymbol)
                fake_hint.text = FiatValue.zero(newValue.inputCurrency.left).toStringWithoutSymbol()
                enter_amount.configuration = Configuration(
                    prefixOrSuffix = inputSymbol,
                    isPrefix = true,
                    initialText = newValue.predefinedAmount.toStringWithoutSymbol()
                        .replace(DecimalFormatSymbols(Locale.getDefault()).groupingSeparator.toString(), "")
                        .removeSuffix("${DecimalFormatSymbols(Locale.getDefault()).decimalSeparator}00")
                )
            } else if (newValue.inputCurrency is Either.Right) {
                //   updateFilters(outputSymbol)
                fake_hint.text = CryptoValue.zero(newValue.inputCurrency.right).toStringWithSymbol()
                enter_amount.configuration = Configuration(
                    prefixOrSuffix = inputSymbol,
                    isPrefix = false,
                    initialText = newValue.predefinedAmount.toStringWithoutSymbol()
                        .replace(DecimalFormatSymbols(Locale.getDefault()).groupingSeparator.toString(), "")
                )
            }
            enter_amount.resetForTyping()
        }
    }

    var maxLimit by Delegates.observable<Money?>(null) { _, oldValue, newValue ->
        /*  if (newValue != oldValue)
              updateFilters(enter_amount.configuration.prefixOrSuffix)*/
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
    /*
        private fun updateFilters(prefixOrSuffix: String) {
            if (configuration.inputCurrency is Either.Left) {
                val maxDecimalDigitsForAmount = maxLimit?.inFiat()?.userDecimalPlaces ?: return
                val maxIntegerDigitsForAmount = maxLimit?.inFiat()?.toStringParts()?.major?.length ?: return
                enter_amount.addFilter(maxDecimalDigitsForAmount, maxIntegerDigitsForAmount, prefixOrSuffix)
            } else {
                val maxDecimalDigitsForAmount = maxLimit?.inCrypto()?.userDecimalPlaces ?: return
                val maxIntegerDigitsForAmount = maxLimit?.inCrypto()?.toStringParts()?.major?.length ?: return
                enter_amount.addFilter(maxDecimalDigitsForAmount, maxIntegerDigitsForAmount, prefixOrSuffix)
            }
        }*/

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

        if (config is Either.Left) {
            // val fiatCurrency = c.left

            /*    val fiatAmount = enter_amount.bigDecimalValue?.let { amount ->
                    FiatValue.fromMajor(configuration.fiatCurrency, amount)
                } ?: FiatValue.zero(configuration.fiatCurrency)

                val cryptoAmount = cryptoToFiatRate.inverse(RoundingMode.CEILING, cryptoCurrency.userDp).convert(fiatAmount)
                exchange_amount.text = cryptoAmount.toStringWithSymbol()
    */
            val enteredAmount = enter_amount.bigDecimalValue?.let { amount ->
                FiatValue.fromMajor(config.left, amount)
            } ?: FiatValue.zero(config.left)

            val exchangeAmount = inputToOutputExchangeRate.convert(enteredAmount)
            val internalExchangeAmount = internalExchangeRate.convert(enteredAmount)
            exchange_amount.text = internalExchangeAmount.toStringWithSymbol()
            amountSubject.onNext(exchangeAmount)
        } else if (config is Either.Right) {

            val cryptoAmount = enter_amount.bigDecimalValue?.let { amount ->
                CryptoValue.fromMajor(config.right, amount)
            } ?: CryptoValue.zero(config.right)

            val exchangeAmount = inputToOutputExchangeRate.convert(cryptoAmount)
            val internalExchangeAmount = internalExchangeRate.convert(cryptoAmount)

            exchange_amount.text = internalExchangeAmount.toStringWithSymbol()
            amountSubject.onNext(exchangeAmount)
        }
    }

    fun fixExchange(it: Money) {
        exchange_amount.text = it.toStringWithSymbol()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        compositeDisposable.clear()
    }

    private fun FiatCryptoViewConfiguration.internalExchgangeRate() =
        exchangeRate(inputCurrency, exchangeCurrency)

    private fun FiatCryptoViewConfiguration.defExchgangeRate() =
        exchangeRate(inputCurrency, outputCurrency)

    private fun exchangeRate(
        input: Either<String, CryptoCurrency>,
        output: Either<String, CryptoCurrency>
    ): ExchangeRate {
        return when (input) {
            is Either.Left -> when (output) {
                is Either.Right -> {
                    ExchangeRate.CryptoToFiat(
                        to = input.left,
                        from = output.right,
                        _rate = defExchangeRateDataManager.getLastPrice(output.right, input.left)
                    ).inverse(RoundingMode.CEILING, output.right.userDp)
                }
                is Either.Left -> {
                    ExchangeRate.FiatToFiat(
                        input.left,
                        output.left,
                        defExchangeRateDataManager.getLastPriceOfFiat(output.left, input.left)
                    )
                }
            }
            is Either.Right -> when (output) {
                is Either.Right -> {
                    ExchangeRate.CryptoToCrypto(
                        from = input.right,
                        to = output.right,
                        rate = if (output.right != input.right) throw NotImplementedError("") else 1.toBigDecimal()
                    )
                }
                is Either.Left -> {
                    ExchangeRate.CryptoToFiat(
                        from = input.right,
                        to = output.left,
                        _rate = defExchangeRateDataManager.getLastPrice(input.right, output.left)
                    )
                }
            }
        }
    }

    fun updateWithMaxValue(maxSpendable: Money) {
        if (configuration.inputCurrency is Either.Left && maxSpendable is CryptoValue) {
            configuration = configuration.copy(
                inputCurrency = Either.Right(maxSpendable.currency),
                exchangeCurrency = configuration.inputCurrency,
                outputCurrency = Either.Right(maxSpendable.currency)
            )
        }
        showValue(
            maxSpendable
        )
    }
}

data class FiatCryptoViewConfiguration(
    val inputCurrency: Either<String, CryptoCurrency>,     // the currency used for input by the user
    val exchangeCurrency: Either<String, CryptoCurrency>,  // the currency used for the exchanged amount
    val outputCurrency: Either<String, CryptoCurrency>,   // the currency used for the model output
    val predefinedAmount: Money = inputCurrency.zeroValue(),
    val canSwap: Boolean = true
) {

    val swapEnabled: Boolean
        get() = canSwap && inputCurrency != exchangeCurrency
}

private fun Either<String, CryptoCurrency>.zeroValue(): Money =
    when (this) {
        is Either.Left -> FiatValue.zero(this.left)
        is Either.Right -> CryptoValue.zero(this.right)
    }

private fun Either<String, CryptoCurrency>.symbol(): String =
    when (this) {
        is Either.Left -> Currency.getInstance(left).getSymbol(Locale.getDefault())
        is Either.Right -> right.displayTicker
    }

sealed class Either<A, B> {
    data class Left<A, B>(val left: A) : Either<A, B>()
    data class Right<A, B>(val right: B) : Either<A, B>()
}