package piuk.blockchain.android.ui.linkbank.yapily.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.databinding.ItemYapilyInfoBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate

class YapilyInfoItemDelegate : AdapterDelegate<YapilyPermissionItem> {
    override fun isForViewType(items: List<YapilyPermissionItem>, position: Int): Boolean =
        items[position] is YapilyPermissionItem.YapilyInfoItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        YapilyInfoItemViewHolder(
            ItemYapilyInfoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(
        items: List<YapilyPermissionItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as YapilyInfoItemViewHolder).bind(items[position] as YapilyPermissionItem.YapilyInfoItem)
}

class YapilyInfoItemViewHolder(
    val binding: ItemYapilyInfoBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: YapilyPermissionItem.YapilyInfoItem) {
        with(binding) {
            infoTitle.text = item.title
            infoDetails.text = item.info
        }
    }
}