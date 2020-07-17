package piuk.blockchain.android.ui.dashboard.sheets

import android.content.Context
import android.view.View
import android.widget.TextView
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.ExchangeRates
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_fiat_funds_detail_sheet.view.*
import kotlinx.android.synthetic.main.item_dashboard_funds.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.NullFiatAccount
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visibleIf

class FiatFundsDetailSheet : SlidingModalBottomDialog() {

    interface Host : SlidingModalBottomDialog.Host {
        fun depositFiat(account: FiatAccount)
        fun gotoActivityFor(account: BlockchainAccount)
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a FiatFundsDetailSheet.Host")
    }

    private val prefs: CurrencyPrefs by scopedInject()
    private val exchangeRates: ExchangeRates by scopedInject()
    private val disposables = CompositeDisposable()

    private var account: FiatAccount = NullFiatAccount

    override val layoutResource: Int
        get() = R.layout.dialog_fiat_funds_detail_sheet

    override fun initControls(view: View) {
        val ticker = account.fiatCurrency
        view.apply {
            funds_title.setStringFromTicker(context, ticker)
            funds_fiat_ticker.text = ticker
            funds_icon.setIcon(ticker)

            funds_balance.gone()
            funds_user_fiat_balance.gone()

            disposables += Singles.zip(
                account.balance,
                account.fiatBalance(prefs.selectedFiatCurrency, exchangeRates)
            ).subscribeBy(
                onSuccess = { (fiatBalance, userFiatBalance) ->
                    funds_user_fiat_balance.visibleIf { prefs.selectedFiatCurrency != ticker }
                    funds_user_fiat_balance.text = userFiatBalance.toStringWithSymbol()

                    funds_balance.text = fiatBalance.toStringWithSymbol()
                }
            )

            funds_deposit_holder.setOnClickListener {
                dismiss()
                host.depositFiat(account)
            }

            funds_activity_holder.setOnClickListener {
                dismiss()
                host.gotoActivityFor(account)
            }
        }
    }

    companion object {
        fun newInstance(fiatAccount: FiatAccount): FiatFundsDetailSheet {
            return FiatFundsDetailSheet().apply {
                account = fiatAccount
            }
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