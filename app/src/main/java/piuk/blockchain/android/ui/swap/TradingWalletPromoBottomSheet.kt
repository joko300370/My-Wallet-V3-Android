package piuk.blockchain.android.ui.swap

import android.view.LayoutInflater
import android.view.ViewGroup
import piuk.blockchain.android.databinding.DialogSheetSwapTradingWalletBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

class TradingWalletPromoBottomSheet : SlidingModalBottomDialog<DialogSheetSwapTradingWalletBinding>() {
    interface Host : SlidingModalBottomDialog.Host {
        fun startNewSwap()
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a TradingWalletPromoBottomSheet.Host"
        )
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetSwapTradingWalletBinding =
        DialogSheetSwapTradingWalletBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogSheetSwapTradingWalletBinding) {
        binding.swapTradingCta.setOnClickListener {
            dismiss()
            host.startNewSwap()
        }
    }

    companion object {
        fun newInstance(): TradingWalletPromoBottomSheet = TradingWalletPromoBottomSheet()
    }
}