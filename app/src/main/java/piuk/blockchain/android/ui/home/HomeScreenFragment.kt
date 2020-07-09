package piuk.blockchain.android.ui.home

import androidx.fragment.app.Fragment
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.ui.base.MvpFragment
import piuk.blockchain.android.ui.base.MvpPresenter
import piuk.blockchain.android.ui.base.MvpView
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.mvi.MviIntent
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState

interface HomeScreenFragment {
    fun navigator(): HomeNavigator
    fun onBackPressed(): Boolean
}

interface HomeNavigator {
    fun showNavigation()
    fun hideNavigation()

    fun gotoDashboard()

    fun launchSwapOrKyc(targetCurrency: CryptoCurrency? = null, fromCryptoCurrency: CryptoCurrency? = null)
    fun launchSwap(
        defCurrency: String,
        fromCryptoCurrency: CryptoCurrency? = null,
        toCryptoCurrency: CryptoCurrency? = null
    )

    fun launchKyc(campaignType: CampaignType)
    fun launchKycIntro()
    fun launchThePitLinking(linkId: String = "")
    fun launchThePit()
    fun launchSimpleBuy()
    fun showProgress()
    fun hideProgress()
    fun launchBackupFunds(fragment: Fragment? = null, requestCode: Int = 0)
    fun launchSetup2Fa()
    fun launchVerifyEmail()
    fun launchSetupFingerprintLogin()
    fun launchTransfer()
    fun launchIntroTour()

    @Deprecated(message = "Switch to accounts")
    fun gotoSendFor(cryptoCurrency: CryptoCurrency)
    fun gotoSendFor(account: SingleAccount)
    fun gotoReceiveFor(account: SingleAccount)
    fun gotoActivityFor(account: BlockchainAccount?)

    fun resumeSimpleBuyKyc()
    fun startSimpleBuy()
}

abstract class HomeScreenMvpFragment<V : MvpView, P : MvpPresenter<V>> : MvpFragment<V, P>(), HomeScreenFragment {

    override fun navigator(): HomeNavigator =
        (activity as? HomeNavigator) ?: throw IllegalStateException("Parent must implement HomeNavigator")
}

abstract class HomeScreenMviFragment<M : MviModel<S, I>, I : MviIntent<S>, S : MviState> : MviFragment<M, I, S>(),
    HomeScreenFragment {

    override fun navigator(): HomeNavigator =
        (activity as? HomeNavigator) ?: throw IllegalStateException("Parent must implement HomeNavigator")
}