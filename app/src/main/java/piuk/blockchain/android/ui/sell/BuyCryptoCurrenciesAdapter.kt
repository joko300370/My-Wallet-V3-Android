package piuk.blockchain.android.ui.sell

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.databinding.BuyCryptoItemLayoutBinding
import piuk.blockchain.android.ui.dashboard.asDeltaPercent

class BuyCryptoCurrenciesAdapter(private val items: List<BuyCryptoItem>, val assetResources: AssetResources) :
    RecyclerView.Adapter<BuyCryptoCurrenciesAdapter.ViewHolder>() {

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            BuyCryptoItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false), assetResources
        )

    class ViewHolder(private val binding: BuyCryptoItemLayoutBinding, val assetResources: AssetResources) :
        RecyclerView.ViewHolder(binding.root) {
        val iconView: AppCompatImageView = binding.icon
        val currency: AppCompatTextView = binding.currency
        val container: View = binding.container
        val priceDelta: TextView = binding.priceDelta
        val price: AppCompatTextView = binding.price
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with(holder) {
            iconView.setImageResource(assetResources.drawableResFilled(item.cryptoCurrency))
            currency.setText(assetResources.assetNameRes(item.cryptoCurrency))
            priceDelta.asDeltaPercent(item.percentageDelta)
            price.text = item.price.toStringWithSymbol()
            container.setOnClickListener {
                item.click()
            }
        }
    }
}