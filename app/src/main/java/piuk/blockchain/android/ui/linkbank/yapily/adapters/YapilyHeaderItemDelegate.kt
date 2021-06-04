package piuk.blockchain.android.ui.linkbank.yapily.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.databinding.ItemYapilyTermsHeaderBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.visibleIf

class YapilyHeaderItemDelegate : AdapterDelegate<YapilyPermissionItem> {
    override fun isForViewType(items: List<YapilyPermissionItem>, position: Int): Boolean =
        items[position] is YapilyPermissionItem.YapilyHeaderItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        YapilyHeaderItemViewHolder(
            ItemYapilyTermsHeaderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(
        items: List<YapilyPermissionItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as YapilyHeaderItemViewHolder).bind(items[position] as YapilyPermissionItem.YapilyHeaderItem)
}

class YapilyHeaderItemViewHolder(
    val binding: ItemYapilyTermsHeaderBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: YapilyPermissionItem.YapilyHeaderItem) {
        with(binding) {
            yapilyHeaderIcon.setImageResource(item.icon)
            yapilyHeaderTitle.text = item.title
            yapilySubheader.visibleIf { item.shouldShowSubheader }
        }
    }
}