package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.enter_fiat_crypto_layout.view.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import piuk.blockchain.android.R
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.toCrypto
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.utils.DecimalDigitsInputFilter
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.extensions.visibleIf
import piuk.blockchain.androidcoreui.utils.helperfunctions.AfterTextChangedWatcher
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale
import kotlin.properties.Delegates

class FiatCryptoInputView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs), KoinComponent {

    private val amountSubject: PublishSubject<Money> = PublishSubject.create()

    val amount: Observable<Money>
        get() = amountSubject

    private val exchangeRateDataManager: ExchangeRateDataManager by scopedInject()
    private val currencyPrefs: CurrencyPrefs by inject()

    private val cryptoCurrency: CryptoCurrency
        get() = configuration.cryptoCurrency ?: throw IllegalStateException("Cryptocurrency not set")

    init {
        inflate(context, R.layout.enter_fiat_crypto_layout, this)

        enter_amount.addTextChangedListener(object : AfterTextChangedWatcher() {
            override fun afterTextChanged(s: Editable?) {
                if (configuration.isInitialised.not())
                    return
                if (configuration.input == CurrencyType.Fiat) {

                    val fiatAmount = enter_amount.majorValue.toBigDecimalOrNull()?.let { amount ->
                        FiatValue.fromMajor(configuration.fiatCurrency, amount)
                    } ?: FiatValue.zero(configuration.fiatCurrency)

                    val cryptoAmount = fiatAmount.toCrypto(exchangeRateDataManager, cryptoCurrency)
                    exchange_amount.text = cryptoAmount.toStringWithSymbol()

                    amountSubject.onNext(
                        if (configuration.output == CurrencyType.Fiat) fiatAmount else cryptoAmount
                    )
                } else {

                    val cryptoAmount = enter_amount.majorValue.toBigDecimalOrNull()?.let { amount ->
                        CryptoValue.fromMajor(cryptoCurrency, amount)
                    } ?: CryptoValue.zero(cryptoCurrency)

                    val fiatAmount = cryptoAmount.toFiat(exchangeRateDataManager, configuration.fiatCurrency)

                    exchange_amount.text = fiatAmount.toStringWithSymbol()
                    amountSubject.onNext(
                        if (configuration.output == CurrencyType.Fiat) fiatAmount else cryptoAmount
                    )
                }
            }
        })

        currency_swap.setOnClickListener {
            configuration =
                configuration.copy(
                    input = configuration.input.swap(),
                    predefinedAmount = getLastEnteredAmount(configuration)
                )
        }
    }

    val isConfigured: Boolean
        get() = configuration.isInitialised

    private fun getLastEnteredAmount(configuration: FiatCryptoViewConfiguration): Money =
        enter_amount.majorValue.toBigDecimalOrNull()?.let { enterAmount ->
            if (configuration.input == CurrencyType.Fiat) FiatValue.fromMajor(configuration.fiatCurrency, enterAmount)
            else CryptoValue.fromMajor(cryptoCurrency, enterAmount)
        } ?: FiatValue.zero(configuration.fiatCurrency)

    var maxLimit by Delegates.observable<Money>(FiatValue.fromMinor(currencyPrefs.defaultFiatCurrency,
        Long.MAX_VALUE)) { _, oldValue, newValue ->
        if (newValue != oldValue)
            updateFilters(enter_amount.configuration.prefixOrSuffix)
    }

    var configuration: FiatCryptoViewConfiguration by Delegates.observable(FiatCryptoViewConfiguration(
        CurrencyType.Fiat, CurrencyType.Crypto, currencyPrefs.selectedFiatCurrency, null)
    ) { _, oldValue, newValue ->
        if (oldValue != newValue) {

            enter_amount.filters = emptyArray()
            val fiatSymbol = Currency.getInstance(newValue.fiatCurrency).getSymbol(Locale.getDefault())
            val cryptoSymbol = cryptoCurrency.displayTicker
            currency_swap.visibleIf { newValue.canSwap }
            if (newValue.input == CurrencyType.Fiat) {
                updateFilters(fiatSymbol)
                enter_amount.configuration = Configuration(
                    prefixOrSuffix = fiatSymbol,
                    isPrefix = true,
                    initialText = newValue.predefinedAmount.inFiat().toStringWithoutSymbol()
                        .replace(DecimalFormatSymbols(Locale.getDefault()).groupingSeparator.toString(), "")
                        .removeSuffix("${DecimalFormatSymbols(Locale.getDefault()).decimalSeparator}00")
                )
            } else {
                updateFilters(cryptoSymbol)
                enter_amount.configuration = Configuration(
                    prefixOrSuffix = cryptoSymbol,
                    isPrefix = false,
                    initialText = newValue.predefinedAmount.inCrypto().toStringWithoutSymbol()
                        .replace(DecimalFormatSymbols(Locale.getDefault()).groupingSeparator.toString(), "")
                )
            }
            amountSubject.onNext(
                if (newValue.output == CurrencyType.Crypto) newValue.predefinedAmount.inCrypto()
                else newValue.predefinedAmount.inFiat()
            )
        }
    }

    fun showError(errorMessage: String) {
        error.text = errorMessage
        error.visible()
        exchange_amount.gone()
        currency_swap.let {
            it.isEnabled = false
            it.alpha = .6f
        }
    }

    fun hideError() {
        error.gone()
        exchange_amount.visible()
        currency_swap.let {
            it.isEnabled = true
            it.alpha = 1f
        }
    }

    private fun updateFilters(prefixOrSuffix: String) {
        if (configuration.input == CurrencyType.Fiat) {
            val maxDecimalDigitsForAmount = maxLimit.inFiat().userDecimalPlaces
            val maxIntegerDigitsForAmount = maxLimit.inFiat().toStringParts().major.length
            enter_amount.addFilter(maxDecimalDigitsForAmount, maxIntegerDigitsForAmount, prefixOrSuffix)
        } else {
            val maxDecimalDigitsForAmount = maxLimit.inCrypto().userDecimalPlaces
            val maxIntegerDigitsForAmount = maxLimit.inCrypto().toStringParts().major.length
            enter_amount.addFilter(maxDecimalDigitsForAmount, maxIntegerDigitsForAmount, prefixOrSuffix)
        }
    }

    private fun PrefixedOrSuffixedEditText.addFilter(
        maxDecimalDigitsForAmount: Int,
        maxIntegerDigitsForAmount: Int,
        prefixOrSuffix: String
    ) {
        filters =
            arrayOf(
                DecimalDigitsInputFilter(
                    maxIntegerDigitsForAmount,
                    maxDecimalDigitsForAmount,
                    prefixOrSuffix
                )
            )
    }

    private fun Money.inFiat(): FiatValue =
        when (this) {
            is CryptoValue -> toFiat(exchangeRateDataManager, configuration.fiatCurrency)
            is FiatValue -> this
            else -> throw IllegalStateException("Illegal money type")
        }

    private fun Money.inCrypto(): CryptoValue =
        when (this) {
            is FiatValue -> toCrypto(exchangeRateDataManager,
                configuration?.cryptoCurrency ?: throw IllegalStateException("Currency not specified"))
            is CryptoValue -> this
            else -> throw IllegalStateException("Illegal money type")
        }

    private fun CurrencyType.swap(): CurrencyType =
        if (this == CurrencyType.Fiat) CurrencyType.Crypto else CurrencyType.Fiat
}

data class FiatCryptoViewConfiguration(
    val input: CurrencyType = CurrencyType.Fiat,
    val output: CurrencyType = CurrencyType.Crypto,
    val fiatCurrency: String,
    val cryptoCurrency: CryptoCurrency?,
    val predefinedAmount: Money = FiatValue.zero(fiatCurrency),
    val canSwap: Boolean = true
) {
    val isInitialised: Boolean by unsafeLazy {
        cryptoCurrency != null
    }
}

enum class CurrencyType {
    Fiat, Crypto
}