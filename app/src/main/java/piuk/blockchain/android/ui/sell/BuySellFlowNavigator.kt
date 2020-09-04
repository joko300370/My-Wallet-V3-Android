package piuk.blockchain.android.ui.sell

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.OrderState
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import piuk.blockchain.android.simplebuy.SimpleBuyModel

class BuySellFlowNavigator(
    private val simpleBuyModel: SimpleBuyModel,
    private val currencyPrefs: CurrencyPrefs,
    private val custodialWalletManager: CustodialWalletManager,
    private val sellFeatureFlag: FeatureFlag
) {
    fun navigateTo(): Single<BuySellIntroAction> = simpleBuyModel.state.firstOrError().flatMap {
        if (it.orderState > OrderState.INITIALISED && it.orderState < OrderState.FINISHED) {
            if (currencyPrefs.selectedFiatCurrency == it.fiatCurrency) {
                Single.just(BuySellIntroAction.NavigateToBuy)
            } else {
                Single.just(BuySellIntroAction.NavigateToCurrencySelection(listOf(
                    it.fiatCurrency
                )))
            }
        } else {
            Singles.zip(custodialWalletManager.isCurrencySupportedForSimpleBuy(currencyPrefs.selectedFiatCurrency),
                custodialWalletManager.getSupportedFiatCurrencies(),
                sellFeatureFlag.enabled) { currencySupported, supportedFiats, sellEnabled ->
                if (currencySupported)
                    BuySellIntroAction.DisplayBuySellIntro(sellEnabled)
                else
                    BuySellIntroAction.NavigateToCurrencySelection(supportedFiats)
            }
        }
    }
}

sealed class BuySellIntroAction {
    object NavigateToBuy : BuySellIntroAction()

    data class NavigateToCurrencySelection(val supportedCurrencies: List<String>) : BuySellIntroAction()

    data class DisplayBuySellIntro(val sellEnabled: Boolean) : BuySellIntroAction()
}