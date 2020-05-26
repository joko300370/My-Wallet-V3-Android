package piuk.blockchain.android.simplebuy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_simple_buy_checkout_info.view.*
import piuk.blockchain.android.R
import kotlin.properties.Delegates

class CheckoutAdapter : RecyclerView.Adapter<CheckoutAdapter.ViewHolder>() {

    var items: List<CheckoutItem> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            notifyDataSetChanged()
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val key: TextView = itemView.title
        val value: TextView = itemView.description
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LayoutInflater.from(parent.context).inflate(
            R.layout.item_simple_buy_checkout_info,
            parent,
            false
        )
        return ViewHolder(layout)
    }

    override fun getItemCount(): Int =
        items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            val item = items[position]
            key.text = item.key
            value.text = item.value
        }
    }
}

data class CheckoutItem(val key: String, val value: String)