package piuk.blockchain.android.ui.activity.detail.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.databinding.ItemFiatActivityDetailsBinding
import piuk.blockchain.android.ui.activity.detail.FiatDetailItem
import kotlin.properties.Delegates

class FiatDetailsSheetAdapter : RecyclerView.Adapter<FiatDetailsSheetAdapter.FiatDetailsViewHolder>() {

    var items: List<FiatDetailItem> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FiatDetailsViewHolder =
        FiatDetailsViewHolder(
            ItemFiatActivityDetailsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: FiatDetailsViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class FiatDetailsViewHolder(private val binding: ItemFiatActivityDetailsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FiatDetailItem) {
            with(binding) {
                title.text = item.key
                description.text = item.value
            }
        }
    }
}