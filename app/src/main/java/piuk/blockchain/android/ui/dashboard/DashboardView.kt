package piuk.blockchain.android.ui.dashboard

import android.support.annotation.StringRes
import android.support.design.widget.BottomSheetDialogFragment
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import java.util.Locale

interface DashboardView : View {

    val locale: Locale

    fun updatePieChartState(chartsState: PieChartsState)

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun notifyItemAdded(displayItems: MutableList<Any>, position: Int)

    fun notifyItemRemoved(displayItems: MutableList<Any>, position: Int)

    fun notifyItemUpdated(displayItems: MutableList<Any>, positions: List<Int>)

    fun startBitcoinCashReceive()

    fun scrollToTop()

    fun startWebsocketService()

    fun startKycFlow(campaignType: CampaignType)

    fun startBuySell()

    fun startSwap(defCurrency: String, currency: CryptoCurrency?)

    fun startPitLinkingFlow(linkId: String = "")

    fun startBackupWallet()

    fun startSetup2Fa()

    fun startEnableFingerprintLogin()

    fun startVerifyEmail()

    fun startIntroTour()

    fun startTransferCrypto()

    fun launchWaitlist()

    fun showBottomSheetDialog(bottomSheetDialogFragment: BottomSheetDialogFragment)
}
