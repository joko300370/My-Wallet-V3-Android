package piuk.blockchain.android.ui.dashboard

import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import android.widget.TextView
import com.elyeproj.loaderviewlibrary.LoaderTextView
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import piuk.blockchain.android.R
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money

fun LoaderTextView.showLoading() =
    resetLoader()

fun Money?.format(fiatSymbol: String) =
    this?.toStringWithSymbol()
        ?: FiatValue.zero(fiatSymbol).toStringWithSymbol()

fun Money?.format(cryptoCurrency: CryptoCurrency) =
    this?.toStringWithSymbol()
        ?: CryptoValue.zero(cryptoCurrency).toStringWithSymbol()

fun Double.asPercentString() =
    String.format("%.2f%%", this)

fun TextView.setDeltaColour(
    delta: Double,
    positiveColor: Int = R.color.dashboard_delta_positive,
    negativeColor: Int = R.color.dashboard_delta_negative
) {
    if (delta < 0)
        setTextColor(ContextCompat.getColor(context, negativeColor))
    else
        setTextColor(ContextCompat.getColor(context, positiveColor))
}

@SuppressLint("SetTextI18n")
fun TextView.asDeltaPercent(delta: Double, prefix: String = "", postfix: String = "") {

    text = prefix + if (delta.isNaN()) {
        "--"
    } else {
        delta.asPercentString()
    } + postfix
    setDeltaColour(delta)
}
