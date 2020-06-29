package piuk.blockchain.android.ui.dashboard.sheets

import android.view.View
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

class FiatFundsKycDetailsSheet : SlidingModalBottomDialog() {

    interface Host : SlidingModalBottomDialog.Host {
        fun onPositiveCta()
        fun onNegativeCta()
    }

    override val layoutResource: Int
        get() = R.layout.dialog_fiat_funds_kyc_details_sheet

    override fun initControls(view: View) {
    }

}