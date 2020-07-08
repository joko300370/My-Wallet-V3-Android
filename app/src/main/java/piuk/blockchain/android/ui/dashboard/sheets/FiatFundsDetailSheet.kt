package piuk.blockchain.android.ui.dashboard.sheets

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.FiatValue
import kotlinx.android.synthetic.main.dialog_fiat_funds_detail_sheet.view.*
import kotlinx.android.synthetic.main.item_dashboard_funds.view.*
import kotlinx.android.synthetic.main.item_dashboard_funds_parent.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.toFiatWithCurrency
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visibleIf

class FiatFundsDetailSheet : SlidingModalBottomDialog() {

    interface Host : SlidingModalBottomDialog.Host {
        fun depositFiat(fiat: FiatValue)
        fun showActivity(fiat: FiatValue)
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a FiatFundsDetailSheet.Host")
    }

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
            funds_title.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.size_standard))

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
            funds_icon.setIcon(ticker)

            funds_deposit_holder.setOnClickListener {
                dismiss()
                host.depositFiat(fiatValue)
            }

            funds_activity_holder.setOnClickListener {
                dismiss()
                host.showActivity(fiatValue)
            }
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
            } ?: throw IllegalStateException(
                "Fiat value can't be null when displaying Fund details")
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
}