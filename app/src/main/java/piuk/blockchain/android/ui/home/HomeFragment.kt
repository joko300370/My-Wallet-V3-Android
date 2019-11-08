package piuk.blockchain.android.ui.home

import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.base.View
import java.lang.IllegalStateException

abstract class HomeFragment<VIEW : View, PRESENTER : BasePresenter<VIEW>> : BaseFragment<VIEW, PRESENTER>() {

    fun navigator(): HomeNavigator =
        (activity as? HomeNavigator) ?: throw IllegalStateException("Parent must implement HomeNavigator")

    /*abstract*/ fun refresh(cryptoCurrency: CryptoCurrency) {}
    abstract fun onBackPressed(): Boolean
}

interface HomeNavigator {
    fun showNavigation()
    fun hideNavigation()

    fun gotoDashboard()

    fun launchSwapOrKyc(targetCurrency: CryptoCurrency? = null)
    fun launchSwap(defCurrency: String, targetCrypto: CryptoCurrency? = null)
    fun launchKyc(campaignType: CampaignType)
    fun launchKycIntro()
    fun launchThePitLinking(linkId: String = "")
    fun launchThePit()
    fun launchBackupFunds()
    fun launchSetup2Fa()
    fun launchVerifyEmail()
    fun launchSetupFingerprintLogin()
    fun launchBuySell()
    fun launchTransfer()
    fun launchIntroTour()

    fun gotoSendFor(cryptoCurrency: CryptoCurrency)
    fun gotoReceiveFor(cryptoCurrency: CryptoCurrency)
    fun gotoTransactionsFor(cryptoCurrency: CryptoCurrency)
}