package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.preferences.CurrencyPrefs
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_change_fiat_currency.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import java.util.Locale
import java.util.Currency

class FiatCurrencyChooserBottomSheet : SlidingModalBottomDialog() {
    private val currencies: List<String> by unsafeLazy {
        arguments?.getStringArrayList(SUPPORTED_FIAT_CURRENCIES_KEY) as? List<String>
            ?: emptyList()
    }

    private val settingsDataManager: SettingsDataManager by inject()
    private val currencyPrefs: CurrencyPrefs by inject()

    private val compositeDisposable = CompositeDisposable()

    private val adapter = CurrenciesAdapter {
        updateFiat(it)
    }

    override val layoutResource: Int
        get() = R.layout.fragment_change_fiat_currency

    override fun initControls(view: View) {
        with(view) {
            recycler.layoutManager = LinearLayoutManager(activity)
            recycler.adapter = adapter
        }
        val locale = Locale.getDefault()

        adapter.items = currencies.map {
            CurrencyItem(
                name = Currency.getInstance(it).getDisplayName(locale),
                symbol = it,
                isAvailable = true,
                isChecked = currencyPrefs.selectedFiatCurrency == it
            )
        }
    }

    private fun updateFiat(item: CurrencyItem) {
        compositeDisposable += settingsDataManager.updateFiatUnit(item.symbol)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                adapter.canSelect = false
            }
            .doAfterTerminate {
                adapter.canSelect = true
            }
            .subscribeBy(onNext = {
                (parentFragment as? ChangeCurrencyHost)?.onFiatCurrencyChanged(it.currency)
                dismiss()
            }, onError = {})
    }

    companion object {
        private const val SUPPORTED_FIAT_CURRENCIES_KEY = "supported_fiat_currencies_key"
        fun newInstance(currencies: List<String>): FiatCurrencyChooserBottomSheet {
            val bundle = Bundle()
            bundle.putStringArrayList(SUPPORTED_FIAT_CURRENCIES_KEY, ArrayList(currencies))
            return FiatCurrencyChooserBottomSheet().apply {
                arguments = bundle
            }
        }
    }
}