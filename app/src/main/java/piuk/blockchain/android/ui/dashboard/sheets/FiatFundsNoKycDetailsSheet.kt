package piuk.blockchain.android.ui.dashboard.sheets

import android.view.View
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import kotlinx.android.synthetic.main.dialog_fiat_funds_kyc_details_sheet.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

class FiatFundsNoKycDetailsSheet : SlidingModalBottomDialog() {

    interface Host : SlidingModalBottomDialog.Host {
        fun fiatFundsVerifyIdentityCta()
    }

    private val prefs: CurrencyPrefs by scopedInject()

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a FiatFundsNoKycDetailsSheet.Host")
    }

    override val layoutResource: Int
        get() = R.layout.dialog_fiat_funds_kyc_details_sheet

    override fun initControls(view: View) {
        view.funds_kyc_default_symbol.setIcon(prefs.selectedFiatCurrency)

        view.funds_kyc_positive_action.setOnClickListener {
            dismiss()
            host.fiatFundsVerifyIdentityCta()
        }

        view.funds_kyc_negative_action.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        fun newInstance(): FiatFundsNoKycDetailsSheet = FiatFundsNoKycDetailsSheet()
    }
}