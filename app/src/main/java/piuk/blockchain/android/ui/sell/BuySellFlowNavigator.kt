package piuk.blockchain.android.ui.sell

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.OrderState
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import piuk.blockchain.android.simplebuy.SimpleBuyModel
import piuk.blockchain.androidcore.utils.extensions.thenSingle

class BuySellFlowNavigator(
    private val simpleBuyModel: SimpleBuyModel,
    private val currencyPrefs: CurrencyPrefs,
    private val custodialWalletManager: CustodialWalletManager,
    private val sellFeatureFlag: FeatureFlag
) {
    fun navigateTo(): Single<BuySellIntroAction> = simpleBuyModel.state.firstOrError().flatMap {
        if (it.orderState > OrderState.PENDING_CONFIRMATION && it.orderState < OrderState.FINISHED) {
            if (currencyPrefs.selectedFiatCurrency == it.fiatCurrency) {
                Single.just(BuySellIntroAction.NavigateToBuy)
            } else {
                Single.just(BuySellIntroAction.NavigateToCurrencySelection(listOf(
                    it.fiatCurrency
                )))
            }
        } else {
            val cancel = if (it.orderState == OrderState.PENDING_CONFIRMATION)
                custodialWalletManager.deleteBuyOrder(it.id
                    ?: throw IllegalStateException("Pending order should always have an id")).onErrorComplete()
            else Completable.complete()
            cancel.thenSingle {
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
}

sealed class BuySellIntroAction {
    object NavigateToBuy : BuySellIntroAction()

    data class NavigateToCurrencySelection(val supportedCurrencies: List<String>) : BuySellIntroAction()

    data class DisplayBuySellIntro(val sellEnabled: Boolean) : BuySellIntroAction()
}
