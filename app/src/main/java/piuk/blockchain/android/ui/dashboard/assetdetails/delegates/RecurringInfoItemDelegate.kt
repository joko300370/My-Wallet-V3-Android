package piuk.blockchain.android.ui.dashboard.assetdetails.delegates

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.databinding.RecurringBuyInfoCardBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsItem

class RecurringBuyInfoItemDelegate(
    private val onCardClicked: () -> Unit
) : AdapterDelegate<AssetDetailsItem> {
    override fun isForViewType(items: List<AssetDetailsItem>, position: Int): Boolean =
        items[position] is AssetDetailsItem.RecurringBuyBanner

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        RecurringBuyInfoCardViewHolder(
            RecurringBuyInfoCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<AssetDetailsItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as RecurringBuyInfoCardViewHolder).bind(onCardClicked)
}

private class RecurringBuyInfoCardViewHolder(
    val binding: RecurringBuyInfoCardBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(onCardClicked: () -> Unit) {
        binding.root.setOnClickListener { onCardClicked() }
    }
}