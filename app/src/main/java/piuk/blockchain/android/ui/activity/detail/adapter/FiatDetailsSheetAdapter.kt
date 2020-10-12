package piuk.blockchain.android.ui.activity.detail.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_simple_buy_checkout_info.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.activity.detail.FiatDetailItem
import kotlin.properties.Delegates

class FiatDetailsSheetAdapter : RecyclerView.Adapter<FiatDetailsSheetAdapter.ViewHolder>() {

    var items: List<FiatDetailItem> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LayoutInflater.from(parent.context).inflate(
            R.layout.item_fiat_activity_details,
            parent,
            false
        )
        return ViewHolder(layout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val key: TextView = itemView.title
        val value: TextView = itemView.description

        fun bind(item: FiatDetailItem) {
            key.text = item.key
            value.text = item.value
        }
    }
}