package piuk.blockchain.android.ui.tour

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import piuk.blockchain.android.util.coinIconWhite
import piuk.blockchain.android.util.colorRes
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import kotlinx.android.synthetic.main.fragment_homebrew_exchange.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.swapold.customviews.ThreePartText
import piuk.blockchain.androidcoreui.ui.base.ToolBarActivity
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class SwapTourFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_homebrew_exchange)

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()

        ExchangeCryptoButtonLayout(
            select_from_account_button,
            select_from_account_text,
            select_from_account_icon
        ).setButtonGraphicsAndTextFromCryptoValue(CryptoValue.ZeroBtc)

        ExchangeCryptoButtonLayout(
            select_to_account_button,
            select_to_account_text,
            select_to_account_icon
        ).setButtonGraphicsAndTextFromCryptoValue(CryptoValue.ZeroEth)

        displayFiatLarge(
            FiatValue.zero("USD"),
            CryptoValue.ZeroBtc,
            0
        )

        balance_title.text = getString(R.string.morph_balance_title, CryptoCurrency.BTC.displayTicker)
        balance_value.text = formatSpendableString()

        base_rate.text = "1 ${CryptoCurrency.BTC.displayTicker} ="
        counter_rate.text = "48.32147365 ETH"
    }

    private fun formatSpendableString(): CharSequence {
        val spendableString = SpannableStringBuilder()

        val fiatString = SpannableString("US$140.00")
        fiatString.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.product_green_medium)),
            0,
            fiatString.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spendableString.append(fiatString)
        spendableString.append(" ")
        spendableString.append(" 0.012375857 BTC")
        return spendableString
    }

    private fun displayFiatLarge(fiatValue: FiatValue, cryptoValue: CryptoValue, decimalCursor: Int) {
        val parts = fiatValue.toStringParts()
        large_value.setText(
            ThreePartText(parts.symbol,
                parts.major,
                if (decimalCursor != 0) parts.minor else "")
        )

        val fromCryptoString = cryptoValue.toStringWithSymbol()
        small_value.text = fromCryptoString
    }

    private fun setupToolbar() {
        (activity as AppCompatActivity).supportActionBar?.let {
            (activity as ToolBarActivity).setupToolbar(it, R.string.morph_new_exchange)
        }
    }

    companion object {
        fun newInstance(): SwapTourFragment {
            return SwapTourFragment()
        }
    }
}

private fun ImageView.setCryptoImageIfZero(cryptoValue: CryptoValue) {
    if (cryptoValue.isZero) {
        val drawable = ContextCompat.getDrawable(this.context, cryptoValue.currency.coinIconWhite())
        setImageDrawable(drawable)
    } else {
        setImageDrawable(null)
    }
}

private class ExchangeCryptoButtonLayout(val button: Button, val textView: TextView, val imageView: ImageView) {

    fun setButtonGraphicsAndTextFromCryptoValue(cryptoValue: CryptoValue) {
        val fromCryptoString = cryptoValue.formatOrSymbolForZero()
        button.setBackgroundResource(cryptoValue.currency.colorRes())

        textView.text = fromCryptoString
        imageView.setCryptoImageIfZero(cryptoValue)
        val params = textView.layoutParams as ConstraintLayout.LayoutParams
        if (cryptoValue.isZero)
            params.width = ConstraintLayout.LayoutParams.WRAP_CONTENT
        else
            params.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        textView.layoutParams = params
    }
}
