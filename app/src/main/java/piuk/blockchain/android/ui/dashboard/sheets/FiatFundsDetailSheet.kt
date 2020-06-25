package piuk.blockchain.android.ui.dashboard.sheets

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.FiatValue
import kotlinx.android.synthetic.main.item_dashboard_funds.view.*
import kotlinx.android.synthetic.main.item_dashboard_funds_parent.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.toFiatWithCurrency
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visibleIf

class FiatFundsDetailSheet: SlidingModalBottomDialog() {
    private val fiatValue: FiatValue by lazy {
        arguments?.getSerializable(FIAT_DATA) as? FiatValue
            ?: throw IllegalArgumentException("No fiat data specified")
    }

    private val prefs: CurrencyPrefs by scopedInject()
    private val exchangeRateDataManager: ExchangeRateDataManager by scopedInject()

    override val layoutResource: Int
        get() = R.layout.dialog_fiat_funds_detail_sheet

    override fun initControls(view: View) {
        val ticker = fiatValue.currencyCode
        view.apply {
            funds_balance_other_fiat.visibleIf { prefs.selectedFiatCurrency != ticker }
            funds_balance_other_fiat.text = fiatValue.toStringWithSymbol()
            funds_list.gone()

            funds_title.setStringFromTicker(context, ticker)
            funds_fiat_ticker.text = ticker
            funds_balance.text = if (prefs.selectedFiatCurrency == ticker) {
                fiatValue.toStringWithSymbol()
            } else {
                fiatValue.toFiatWithCurrency(exchangeRateDataManager,
                    prefs.selectedFiatCurrency)
                    .toStringWithSymbol()
            }
            funds_icon.setDrawableFromTicker(context, ticker)
        }
    }

    companion object {
        private const val FIAT_DATA = "fiat_data"

        fun newInstance(fiatValue: FiatValue?): FiatFundsDetailSheet {
            fiatValue?.let {
                return FiatFundsDetailSheet().apply {
                    arguments = Bundle().apply {
                        putSerializable(FIAT_DATA, fiatValue)
                    }
                }
            } ?: throw  IllegalStateException("Fiat value can't be null when displaying Fund details")
        }
    }

    private fun TextView.setStringFromTicker(context: Context, ticker: String) {
        text = context.getString(
            when (ticker) {
                "EUR" -> R.string.euros
                "GBP" -> R.string.pounds
                else -> R.string.empty
            }
        )
    }

    private fun ImageView.setDrawableFromTicker(context: Context, ticker: String) {
        setImageDrawable(
            ContextCompat.getDrawable(context,
                when (ticker) {
                    "EUR" -> R.drawable.ic_vector_euro
                    "GBP" -> R.drawable.ic_vector_pound
                    else -> android.R.drawable.menuitem_background
                }
            ))
    }
}