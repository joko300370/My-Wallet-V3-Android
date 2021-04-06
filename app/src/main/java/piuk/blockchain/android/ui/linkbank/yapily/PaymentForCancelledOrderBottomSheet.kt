package piuk.blockchain.android.ui.linkbank.yapily

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogSheetCancelledOrderBinding
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

class PaymentForCancelledOrderBottomSheet : SlidingModalBottomDialog<DialogSheetCancelledOrderBinding>() {

    private val syncFactory: SimpleBuySyncFactory by scopedInject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetCancelledOrderBinding =
        DialogSheetCancelledOrderBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogSheetCancelledOrderBinding) {
        syncFactory.currentState()?.let {
            binding.transactionProgressView.showFiatTxSuccess(
                getString(R.string.yapily_payment_to_fiat_wallet_title, it.fiatCurrency),
                getString(
                    R.string.yapily_payment_to_fiat_wallet_subtitle,
                    it.selectedCryptoCurrency?.displayTicker ?: getString(
                        R.string.yapily_payment_to_fiat_wallet_default
                    ),
                    it.fiatCurrency
                ),
                it.fiatCurrency
            )
            binding.transactionProgressView.onCtaClick {
                dismiss()
            }
        } ?: dismiss()
    }

    companion object {
        fun newInstance() = PaymentForCancelledOrderBottomSheet()
    }
}