package piuk.blockchain.android.simplebuy

import piuk.blockchain.android.ui.base.FlowFragment
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

interface SimpleBuyScreen : SlidingModalBottomDialog.Host, FlowFragment {
    fun navigator(): SimpleBuyNavigator

    override fun onSheetClosed() {}
}

interface SimpleBuyNavigator {
    fun exitSimpleBuyFlow()
    fun goToBuyCryptoScreen(addToBackStack: Boolean = true)
    fun goToCheckOutScreen(addToBackStack: Boolean = true)
    fun goToCurrencySelection(addToBackStack: Boolean = true)
    fun goToKycVerificationScreen(addToBackStack: Boolean = true)
    fun goToBankDetailsScreen(addToBackStack: Boolean = true)
    fun goToPendingOrderScreen()
    fun startKyc()
    fun hasMoreThanOneFragmentInTheStack(): Boolean
    fun goToCardPaymentScreen(addToBackStack: Boolean = true)
    fun launchIntro()
}