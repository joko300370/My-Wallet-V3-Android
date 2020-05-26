package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import info.blockchain.balance.CryptoCurrency
import kotlinx.android.synthetic.main.layout_crypto_currency_chooser_item.view.*
import kotlinx.android.synthetic.main.simple_buy_crypto_currency_chooser.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.util.assetName
import piuk.blockchain.android.util.drawableResFilled
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import java.io.Serializable

class CryptoCurrencyChooserBottomSheet : SlidingModalBottomDialog() {

    private val cryptoCurrencies: List<CryptoCurrency> by unsafeLazy {
        arguments?.getSerializable(SUPPORTED_CURRENCIES_KEY) as? List<CryptoCurrency> ?: emptyList()
    }

    override val layoutResource: Int
        get() = R.layout.simple_buy_crypto_currency_chooser

    override fun initControls(view: View) {
        view.recycler.adapter =
            BottomSheetCryptoCurrenciesAdapter(
                cryptoCurrencies
                    .map {
                        BottomSheetAdapterItem(it) {
                            (parentFragment as? ChangeCurrencyHost)?.onCryptoCurrencyChanged(it)
                            dismiss()
                        }
                    })
        view.recycler.layoutManager = LinearLayoutManager(context)
    }

    companion object {
        private const val SUPPORTED_CURRENCIES_KEY = "supported_currencies_key"
        fun newInstance(cryptoCurrencies: List<CryptoCurrency>): CryptoCurrencyChooserBottomSheet {
            val bundle = Bundle()
            bundle.putSerializable(SUPPORTED_CURRENCIES_KEY, cryptoCurrencies as Serializable)
            return CryptoCurrencyChooserBottomSheet().apply {
                arguments = bundle
            }
        }
    }
}

private class BottomSheetCryptoCurrenciesAdapter(private val adapterItems: List<BottomSheetAdapterItem>) :
    RecyclerView.Adapter<BottomSheetCryptoCurrenciesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(
                R.layout.layout_crypto_currency_chooser_item,
                parent,
                false
            )
        return ViewHolder(itemView)
    }

    override fun getItemCount(): Int =
        adapterItems.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = adapterItems[position]
        with(holder) {
            iconView.setImageResource(item.cryptoCurrency.drawableResFilled())
            textView.setText(item.cryptoCurrency.assetName())
            container.setOnClickListener { item.clickAction() }
        }
    }

    private class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconView: AppCompatImageView = itemView.icon
        val textView: AppCompatTextView = itemView.text
        val container: View = itemView.container
    }
}

private data class BottomSheetAdapterItem(val cryptoCurrency: CryptoCurrency, val clickAction: () -> Unit)
