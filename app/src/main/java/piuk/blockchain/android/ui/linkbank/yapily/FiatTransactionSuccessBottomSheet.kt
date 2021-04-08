package piuk.blockchain.android.ui.linkbank.yapily

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import piuk.blockchain.android.databinding.DialogSheetCancelledOrderBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

class FiatTransactionSuccessBottomSheet : SlidingModalBottomDialog<DialogSheetCancelledOrderBinding>() {

    private val fiatCurrency: String by lazy {
        arguments?.getString(KEY_CURRENCY, "") ?: ""
    }

    private val title: String by lazy {
        arguments?.getString(KEY_TITLE, "") ?: ""
    }

    private val subtitle: String by lazy {
        arguments?.getString(KEY_SUBTITLE, "") ?: ""
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetCancelledOrderBinding =
        DialogSheetCancelledOrderBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogSheetCancelledOrderBinding) {
        binding.transactionProgressView.showFiatTxSuccess(title, subtitle, fiatCurrency)
        binding.transactionProgressView.onCtaClick {
            dismiss()
        }
    }

    companion object {
        private const val KEY_CURRENCY = "KEY_CURRENCY"
        private const val KEY_TITLE = "KEY_TITLE"
        private const val KEY_SUBTITLE = "KEY_SUBTITLE "
        fun newInstance(currency: String, title: String, subtitle: String) =
            FiatTransactionSuccessBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(KEY_CURRENCY, currency)
                    putString(KEY_TITLE, title)
                    putString(KEY_SUBTITLE, subtitle)
                }
            }
    }
}