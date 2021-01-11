package piuk.blockchain.android.ui.activity.adapter

import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoCurrency
import io.reactivex.disposables.CompositeDisposable
import piuk.blockchain.android.ui.activity.CryptoActivityType
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter

class ActivitiesDelegateAdapter(
    disposables: CompositeDisposable,
    prefs: CurrencyPrefs,
    onCryptoItemClicked: (CryptoCurrency, String, CryptoActivityType) -> Unit,
    onFiatItemClicked: (String, String) -> Unit,
    analytics: Analytics
) : DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {

    init {
        // Add all necessary AdapterDelegate objects here
        with(delegatesManager) {
            addAdapterDelegate(NonCustodialActivityItemDelegate(disposables, prefs, onCryptoItemClicked))
            addAdapterDelegate(SwapActivityItemDelegate(onCryptoItemClicked))
            addAdapterDelegate(CustodialTradingActivityItemDelegate(onCryptoItemClicked))
            addAdapterDelegate(SellActivityItemDelegate(onCryptoItemClicked))
            addAdapterDelegate(CustodialFiatActivityItemDelegate(onFiatItemClicked))
            addAdapterDelegate(CustodialInterestActivityItemDelegate(disposables, prefs, onCryptoItemClicked))
        }
    }
}
