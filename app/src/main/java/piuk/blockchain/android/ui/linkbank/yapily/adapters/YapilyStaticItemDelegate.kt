package piuk.blockchain.android.ui.linkbank.yapily.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.databinding.ItemYapilyStaticBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.context

class YapilyStaticItemDelegate : AdapterDelegate<YapilyPermissionItem> {
    override fun isForViewType(items: List<YapilyPermissionItem>, position: Int): Boolean =
        items[position] is YapilyPermissionItem.YapilyStaticItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        YapilyStaticAgreementItemViewHolder(
            ItemYapilyStaticBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(
        items: List<YapilyPermissionItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as YapilyStaticAgreementItemViewHolder).bind(items[position] as YapilyPermissionItem.YapilyStaticItem)
}

class YapilyStaticAgreementItemViewHolder(
    val binding: ItemYapilyStaticBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: YapilyPermissionItem.YapilyStaticItem) {
        with(binding) {
            staticAgreementBlurb.text = context.getString(item.blurb, item.bankName)
        }
    }
}