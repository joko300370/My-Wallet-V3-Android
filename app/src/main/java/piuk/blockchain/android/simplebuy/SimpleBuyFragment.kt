package piuk.blockchain.android.simplebuy

import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.simplebuy.yodlee.YodleeLinkingFlowNavigator
import piuk.blockchain.android.ui.base.FlowFragment
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

interface SimpleBuyScreen : SlidingModalBottomDialog.Host, FlowFragment {
    fun navigator(): SimpleBuyNavigator

    override fun onSheetClosed() {}
}

interface SimpleBuyNavigator : SlidingModalBottomDialog.Host, YodleeLinkingFlowNavigator {
    fun exitSimpleBuyFlow()
    fun goToBuyCryptoScreen(addToBackStack: Boolean = true, preselectedCrypto: CryptoCurrency)
    fun goToCheckOutScreen(addToBackStack: Boolean = true)
    fun goToKycVerificationScreen(addToBackStack: Boolean = true)
    fun goToPendingOrderScreen()
    fun startKyc()
    fun pop()
    fun hasMoreThanOneFragmentInTheStack(): Boolean
    fun goToCardPaymentScreen(addToBackStack: Boolean = true)
    fun launchIntro()
}