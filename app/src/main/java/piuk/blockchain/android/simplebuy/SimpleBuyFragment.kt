package piuk.blockchain.android.simplebuy

import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

interface SimpleBuyScreen : SlidingModalBottomDialog.Host {
    fun navigator(): SimpleBuyNavigator
    // methods for handling back press and navigation back arrow
    // return true if we want the screen to get popped or handle the back press itself
    fun onBackPressed(): Boolean

    // return true if we want the screen to handle the back press and not get popped
    fun backPressedHandled(): Boolean = false

    override fun onSheetClosed() {}
}

interface SimpleBuyNavigator {
    fun exitSimpleBuyFlow()
    fun goToBuyCryptoScreen(addToBackStack: Boolean = true)
    fun goToCheckOutScreen(addToBackStack: Boolean = true)
    fun goToKycVerificationScreen(addToBackStack: Boolean = true)
    fun goToBankDetailsScreen(addToBackStack: Boolean = true)
    fun startKyc()
    fun launchIntro()
}