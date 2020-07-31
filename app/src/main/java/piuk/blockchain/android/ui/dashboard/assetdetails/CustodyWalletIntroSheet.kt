package piuk.blockchain.android.ui.dashboard.assetdetails

import android.view.View
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.SimpleBuyAnalytics
import com.blockchain.preferences.DashboardPrefs
import kotlinx.android.synthetic.main.dialog_custodial_intro.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

class CustodyWalletIntroSheet : SlidingModalBottomDialog() {

    override val layoutResource: Int = R.layout.dialog_custodial_intro
    private val dashboardPrefs: DashboardPrefs by scopedInject()
    private val model: AssetDetailsModel by scopedInject()

    override fun initControls(view: View) {
        analytics.logEvent(SimpleBuyAnalytics.CUSTODY_WALLET_CARD_SHOWN)
        view.cta_button.setOnClickListener { onCtaClick() }
        //dashboardPrefs.isCustodialIntroSeen = true
    }

    override fun onSheetHidden() {
        super.onSheetHidden()
        dialog?.let {
            onCancel(it)
        }
        model.process(ShowAssetDetailsIntent)
    }

    private fun onCtaClick() {
        analytics.logEvent(SimpleBuyAnalytics.CUSTODY_WALLET_CARD_CLICKED)
        dismiss()
        model.process(ShowAssetDetailsIntent)
    }

    companion object {
        fun newInstance(): CustodyWalletIntroSheet =
            CustodyWalletIntroSheet()
    }
}
