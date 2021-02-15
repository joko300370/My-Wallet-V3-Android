package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.SimpleBuyCurrencyNotSupportedBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import java.lang.IllegalStateException

class CurrencyNotSupportedBottomSheet : SlidingModalBottomDialog<SimpleBuyCurrencyNotSupportedBinding>() {

    private val currencyItem by unsafeLazy {
        (arguments?.getParcelable(CURRENCY_ITEM) as? CurrencyItem)
            ?: throw IllegalStateException("No currency item provided")
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): SimpleBuyCurrencyNotSupportedBinding =
        SimpleBuyCurrencyNotSupportedBinding.inflate(inflater, container, false)

    override fun initControls(binding: SimpleBuyCurrencyNotSupportedBinding) {
        with(binding) {
            title.text = getString(R.string.currency_not_supported_title)
            subtitle.text = getString(R.string.currency_not_supported_1, currencyItem.name)
            skip.setOnClickListener {
                (parentFragment as? ChangeCurrencyOptionHost)?.skip()
                dismiss()
            }
            changeCurrency.setOnClickListener {
                (parentFragment as? ChangeCurrencyOptionHost)?.needsToChange()
                dismiss()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        analytics.logEvent(SimpleBuyAnalytics.CURRENCY_NOT_SUPPORTED_SHOWN)
    }

    companion object {
        private const val CURRENCY_ITEM = "CURRENCY_ITEM_KEY"

        fun newInstance(item: CurrencyItem): CurrencyNotSupportedBottomSheet =
            CurrencyNotSupportedBottomSheet().apply {
                arguments =
                    Bundle().apply { putParcelable(CURRENCY_ITEM, item) }
            }
    }
}