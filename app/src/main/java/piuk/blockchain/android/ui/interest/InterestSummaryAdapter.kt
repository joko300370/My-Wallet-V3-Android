package piuk.blockchain.android.ui.interest

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.databinding.ItemInterestSummaryBinding
import kotlin.properties.Delegates

class InterestSummaryAdapter : RecyclerView.Adapter<InterestSummaryAdapter.ViewHolder>() {

    var items: List<InterestSummarySheet.InterestSummaryInfoItem> by Delegates.observable(
        emptyList()
    ) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            notifyDataSetChanged()
        }
    }

    class ViewHolder(private val binding: ItemInterestSummaryBinding) : RecyclerView.ViewHolder(binding.root) {
        val key: TextView = binding.title
        val value: TextView = binding.description
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemInterestSummaryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int =
        items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            val item = items[position]
            key.text = item.title
            value.text = item.label
        }
    }
}
