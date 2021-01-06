package piuk.blockchain.android.simplebuy

import com.blockchain.nabu.models.data.LinkBankTransfer
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.ui.base.FlowFragment
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

interface SimpleBuyScreen : SlidingModalBottomDialog.Host, FlowFragment {
    fun navigator(): SimpleBuyNavigator

    override fun onSheetClosed() {}
}

interface SimpleBuyNavigator : SlidingModalBottomDialog.Host {
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
    fun launchYodleeSplash(fastLinkUrl: String, accessToken: String, configName: String)
    fun launchYodleeWebview(fastLinkUrl: String, accessToken: String, configName: String)
    fun linkBankWithPartner(bankTransfer: LinkBankTransfer)
    fun launchBankLinking(accountProviderId: String, accountId: String)
    fun launchBankLinkingWithError(errorState: ErrorState)
}