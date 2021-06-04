package piuk.blockchain.android.ui.home

import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.mvi.MviIntent
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState
import piuk.blockchain.android.ui.linkbank.BankLinkingInfo
import piuk.blockchain.android.ui.sell.BuySellFragment

interface HomeScreenFragment {
    fun navigator(): HomeNavigator
    fun onBackPressed(): Boolean
}

interface HomeNavigator {
    fun gotoDashboard()

    fun tryTolaunchSwap(
        sourceAccount: CryptoAccount? = null,
        targetAccount: CryptoAccount? = null
    )

    fun launchKyc(campaignType: CampaignType)
    fun launchThePitLinking(linkId: String = "")
    fun launchThePit()
    fun launchBackupFunds(fragment: Fragment? = null, requestCode: Int = 0)
    fun launchSetup2Fa()
    fun launchVerifyEmail()
    fun launchSetupFingerprintLogin()
    fun launchReceive()
    fun launchSend()
    fun performAssetActionFor(action: AssetAction, account: BlockchainAccount)

    fun launchIntroTour()
    fun launchSimpleBuySell(viewType: BuySellFragment.BuySellViewType = BuySellFragment.BuySellViewType.TYPE_BUY)
    fun launchOpenBankingLinking(bankLinkingInfo: BankLinkingInfo)
    fun launchSimpleBuyFromDeepLinkApproval()
    fun handlePaymentForCancelledOrder(state: SimpleBuyState)
    fun showOpenBankingDeepLinkError()

    fun goToTransfer()

    fun resumeSimpleBuyKyc()
    fun startSimpleBuy(cryptoCurrency: CryptoCurrency)
    fun startInterestDashboard()
}

abstract class HomeScreenMviFragment<M : MviModel<S, I>, I : MviIntent<S>, S : MviState, E : ViewBinding> :
    MviFragment<M, I, S, E>(),
    HomeScreenFragment {

    override fun navigator(): HomeNavigator =
        (activity as? HomeNavigator) ?: throw IllegalStateException("Parent must implement HomeNavigator")
}