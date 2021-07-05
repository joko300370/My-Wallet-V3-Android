package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.ui.trackProgress
import info.blockchain.wallet.api.data.Settings.Companion.UNIT_FIAT
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.parcelize.Parcelize
import org.koin.android.ext.android.inject
import piuk.blockchain.android.databinding.FragmentSimpleBuyCurrencySelectionBinding
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import java.util.Locale
import java.util.Currency

class SimpleBuySelectCurrencyFragment :
    MviBottomSheet<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState, FragmentSimpleBuyCurrencySelectionBinding>(),
    ChangeCurrencyOptionHost {

    private val currencyPrefs: CurrencyPrefs by inject()
    private val settingsDataManager: SettingsDataManager by scopedInject()
    private val appUtil: AppUtil by inject()
    private val compositeDisposable = CompositeDisposable()

    private val currencies: List<String> by unsafeLazy {
        arguments?.getStringArrayList(CURRENCIES_KEY) ?: emptyList<String>()
    }

    // show all currencies if passed list is empty
    private var filter: (CurrencyItem) -> Boolean =
        { if (currencies.isEmpty()) true else currencies.contains(it.symbol) }

    private val adapter = CurrenciesAdapter(true) {
        updateFiat(it)
    }

    override fun initBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSimpleBuyCurrencySelectionBinding =
        FragmentSimpleBuyCurrencySelectionBinding.inflate(inflater, container, false)

    override fun initControls(binding: FragmentSimpleBuyCurrencySelectionBinding) {
        analytics.logEvent(SimpleBuyAnalytics.SELECT_YOUR_CURRENCY_SHOWN)
        with(binding) {
            recycler.layoutManager = LinearLayoutManager(activity)
            recycler.adapter = adapter
        }
        model.process(SimpleBuyIntent.FetchSupportedFiatCurrencies)
    }

    private fun updateFiat(item: CurrencyItem) {
        compositeDisposable += settingsDataManager.updateFiatUnit(item.symbol)
            .trackProgress(appUtil.activityIndicator)
            .doOnSubscribe {
                adapter.canSelect = false
            }
            .doAfterTerminate {
                adapter.canSelect = true
            }
            .subscribeBy(onNext = {
                if (item.isAvailable) {
                    dismiss()
                } else {
                    showCurrencyNotAvailableBottomSheet(item)
                }
                analytics.logEvent(CurrencySelected(item.symbol))
            }, onError = {})
    }

    override fun onResume() {
        super.onResume()
        adapter.canSelect = true
    }

    private fun showCurrencyNotAvailableBottomSheet(item: CurrencyItem) {
        CurrencyNotSupportedBottomSheet.newInstance(item).show(childFragmentManager, "BOTTOM_SHEET")
    }

    override val model: SimpleBuyModel by scopedInject()
    private val locale = Locale.getDefault()

    override fun render(newState: SimpleBuyState) {
        // we need to show the supported currencies only when supported are fetched so we avoid list flickering
        if (newState.supportedFiatCurrencies.isEmpty() && newState.errorState == null)
            return
        adapter.items = UNIT_FIAT.map {
            CurrencyItem(
                name = Currency.getInstance(it).getDisplayName(locale),
                symbol = it,
                isAvailable = newState.supportedFiatCurrencies.contains(it),
                isChecked = currencyPrefs.selectedFiatCurrency == it
            )
        }.sortedWith(compareBy<CurrencyItem> { !it.isAvailable }.thenBy { it.name }).filter(filter)
    }

    override fun navigator(): SimpleBuyNavigator = (activity as? SimpleBuyNavigator)
        ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    override fun onBackPressed(): Boolean = true

    override fun needsToChange() {
        analytics.logEvent(SimpleBuyAnalytics.CURRENCY_NOT_SUPPORTED_CHANGE)
        filter = { it.isAvailable || it.isChecked }
        adapter.items = adapter.items.filter(filter)
    }

    override fun skip() {
        analytics.logEvent(SimpleBuyAnalytics.CURRENCY_NOT_SUPPORTED_SKIP)
        navigator().exitSimpleBuyFlow()
    }

    companion object {
        private const val CURRENCIES_KEY = "CURRENCIES_KEY"
        fun newInstance(currencies: List<String> = emptyList()): SimpleBuySelectCurrencyFragment {
            return SimpleBuySelectCurrencyFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList(CURRENCIES_KEY, ArrayList(currencies))
                }
            }
        }
    }
}

@Parcelize
data class CurrencyItem(
    val name: String,
    val symbol: String,
    val isAvailable: Boolean,
    var isChecked: Boolean = false
) : Parcelable

interface ChangeCurrencyOptionHost : SimpleBuyScreen {
    fun needsToChange()
    fun skip()
}