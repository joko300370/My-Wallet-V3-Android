package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.notifications.analytics.CurrencySelected
import com.blockchain.notifications.analytics.SimpleBuyAnalytics
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.ui.trackLoading
import info.blockchain.wallet.api.data.Settings.UNIT_FIAT
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_simple_buy_currency_selection.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import java.util.Locale
import java.util.Currency

class SimpleBuySelectCurrencyFragment : MviFragment<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState>(),
    ChangeCurrencyOptionHost {

    private val currencyPrefs: CurrencyPrefs by inject()
    private val settingsDataManager: SettingsDataManager by inject()
    private val appUtil: AppUtil by inject()
    private val compositeDisposable = CompositeDisposable()
    private var filter: (CurrencyItem) -> Boolean = { true }

    private val adapter = CurrenciesAdapter(true) {
        updateFiat(it)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_simple_buy_currency_selection)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.setupToolbar(R.string.simple_buy_select_currency)
        analytics.logEvent(SimpleBuyAnalytics.SELECT_YOUR_CURRENCY_SHOWN)
        recycler.layoutManager = LinearLayoutManager(activity)
        recycler.adapter = adapter
        model.process(SimpleBuyIntent.FlowCurrentScreen(FlowScreen.ENTER_AMOUNT))
        model.process(SimpleBuyIntent.FetchSupportedFiatCurrencies)
    }

    private fun updateFiat(item: CurrencyItem) {
        compositeDisposable += settingsDataManager.updateFiatUnit(item.symbol)
            .trackLoading(appUtil.activityIndicator)
            .doOnSubscribe {
                adapter.canSelect = false
            }
            .doAfterTerminate {
                adapter.canSelect = true
            }
            .subscribeBy(onNext = {
                if (item.isAvailable) {
                    navigator().goToBuyCryptoScreen()
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
        showBottomSheet(CurrencyNotSupportedBottomSheet.newInstance(item))
    }

    override val model: SimpleBuyModel by inject()
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