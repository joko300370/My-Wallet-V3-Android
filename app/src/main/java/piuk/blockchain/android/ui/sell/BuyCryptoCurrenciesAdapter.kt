package piuk.blockchain.android.ui.sell

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.buy_crypto_item_layout.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.ui.dashboard.asDeltaPercent

class BuyCryptoCurrenciesAdapter(private val items: List<BuyCryptoItem>, val assetResources: AssetResources) :
    RecyclerView.Adapter<BuyCryptoCurrenciesAdapter.ViewHolder>() {

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(
                R.layout.buy_crypto_item_layout,
                parent,
                false
            )
        return ViewHolder(itemView, assetResources)
    }

    class ViewHolder(itemView: View, val assetResources: AssetResources) : RecyclerView.ViewHolder(itemView) {
        val iconView: AppCompatImageView = itemView.icon
        val currency: AppCompatTextView = itemView.currency
        val container: View = itemView.container
        val priceDelta: TextView = itemView.price_delta
        val price: AppCompatTextView = itemView.price
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