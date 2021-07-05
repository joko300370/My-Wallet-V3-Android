package piuk.blockchain.android.ui.linkbank.yapily

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import piuk.blockchain.android.databinding.DialogSheetFiatTransactionBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.linkbank.FiatTransactionState

class FiatTransactionBottomSheet : SlidingModalBottomDialog<DialogSheetFiatTransactionBinding>() {

    private val fiatCurrency: String by lazy {
        arguments?.getString(KEY_CURRENCY, "").orEmpty()
    }

    private val title: String by lazy {
        arguments?.getString(KEY_TITLE, "").orEmpty()
    }

    private val subtitle: String by lazy {
        arguments?.getString(KEY_SUBTITLE, "").orEmpty()
    }

    private val transactionState: FiatTransactionState by lazy {
        arguments?.getSerializable(KEY_STATE) as FiatTransactionState
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetFiatTransactionBinding =
        DialogSheetFiatTransactionBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogSheetFiatTransactionBinding) {
        with(binding) {
            when (transactionState) {
                FiatTransactionState.SUCCESS -> transactionProgressView.showFiatTxSuccess(
                    title, subtitle, fiatCurrency
                )
                FiatTransactionState.ERROR -> transactionProgressView.showFiatTxError(
                    title, subtitle, fiatCurrency
                )
                FiatTransactionState.PENDING -> transactionProgressView.showFiatTxPending(
                    title, subtitle, fiatCurrency
                )
            }
            transactionProgressView.onCtaClick { dismiss() }
        }
    }

    companion object {
        private const val KEY_CURRENCY = "KEY_CURRENCY"
        private const val KEY_TITLE = "KEY_TITLE"
        private const val KEY_SUBTITLE = "KEY_SUBTITLE"
        private const val KEY_STATE = "KEY_STATE"

        fun newInstance(currency: String, title: String, subtitle: String, state: FiatTransactionState) =
            FiatTransactionBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(KEY_CURRENCY, currency)
                    putString(KEY_TITLE, title)
                    putString(KEY_SUBTITLE, subtitle)
                    putSerializable(KEY_STATE, state)
                }
            }
    }
}