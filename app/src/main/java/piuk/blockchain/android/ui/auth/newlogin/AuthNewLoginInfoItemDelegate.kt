package piuk.blockchain.android.ui.auth.newlogin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.databinding.ItemListInfoRowBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate

class NewLoginAuthInfoItemDelegate : AdapterDelegate<AuthNewLoginDetailsType> {
    override fun isForViewType(items: List<AuthNewLoginDetailsType>, position: Int): Boolean {
        return true
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        InfoItemViewHolder(
            binding = ItemListInfoRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<AuthNewLoginDetailsType>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) =
        (holder as InfoItemViewHolder).bind(items[position])
}

private class InfoItemViewHolder(binding: ItemListInfoRowBinding) : RecyclerView.ViewHolder(binding.root) {

    private val rowHeader = binding.itemListInfoRowTitle
    private val rowValue = binding.itemListInfoRowDescription

    fun bind(infoType: AuthNewLoginDetailsType) {
        rowHeader.text = itemView.resources.getString(infoType.headerTextRes)
        rowValue.text = infoType.value
    }
}