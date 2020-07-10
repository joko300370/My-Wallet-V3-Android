package piuk.blockchain.android.ui.accounts

import com.blockchain.preferences.CurrencyPrefs
import io.reactivex.disposables.CompositeDisposable
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

class AccountsDelegateAdapter(
    disposables: CompositeDisposable,
    private val exchangeRates: ExchangeRateDataManager,
    private val currencyPrefs: CurrencyPrefs,
    onAccountClicked: (BlockchainAccount) -> Unit
) : DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {

    init {
        // Add all necessary AdapterDelegate objects here
        with(delegatesManager) {
            addAdapterDelegate(
                CryptoAccountDelegate(
                    disposables,
                    exchangeRates,
                    currencyPrefs,
                    onAccountClicked
                )
            )
            addAdapterDelegate(
                AllWalletsAccountDelegate(
                    disposables,
                    exchangeRates,
                    currencyPrefs,
                    onAccountClicked
                )
            )
        }
    }
}