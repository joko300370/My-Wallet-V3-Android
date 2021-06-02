package piuk.blockchain.android.simplebuy

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.databinding.ItemSimpleBuyCheckoutInfoBinding
import kotlin.properties.Delegates

class CheckoutAdapter : RecyclerView.Adapter<CheckoutAdapter.ViewHolder>() {

    var items: List<CheckoutItem> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            notifyDataSetChanged()
        }
    }

    class ViewHolder(binding: ItemSimpleBuyCheckoutInfoBinding) : RecyclerView.ViewHolder(binding.root) {
        val key: TextView = binding.title
        val value: TextView = binding.description
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ItemSimpleBuyCheckoutInfoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

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