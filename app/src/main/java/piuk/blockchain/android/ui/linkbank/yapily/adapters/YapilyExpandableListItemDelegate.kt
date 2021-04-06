package piuk.blockchain.android.ui.linkbank.yapily.adapters

import android.graphics.drawable.Animatable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemYapilyExpandableListBinding
import piuk.blockchain.android.databinding.ItemYapilyInfoBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.visibleIf
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor

class YapilyExpandableListItemDelegate(
    private val onExpandableItemClicked: (Int) -> Unit
) : AdapterDelegate<YapilyPermissionItem> {
    override fun isForViewType(items: List<YapilyPermissionItem>, position: Int): Boolean =
        items[position] is YapilyPermissionItem.YapilyExpandableListItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        YapilyExpandableListItemViewHolder(
            ItemYapilyExpandableListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            onExpandableItemClicked
        )

    override fun onBindViewHolder(
        items: List<YapilyPermissionItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as YapilyExpandableListItemViewHolder).bind(
        items[position] as YapilyPermissionItem.YapilyExpandableListItem, position
    )
}

class YapilyExpandableListItemViewHolder(
    val binding: ItemYapilyExpandableListBinding,
    private val onExpandableItemClicked: (Int) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private val infoItemsAdapter = ItemListAdapter()

    init {
        with(binding) {
            expandableList.apply {
                adapter = infoItemsAdapter
                layoutManager = LinearLayoutManager(context)
                addItemDecoration(BlockchainListDividerDecor(context))
            }
        }
    }

    fun bind(item: YapilyPermissionItem.YapilyExpandableListItem, position: Int) {
        with(binding) {
            infoItemsAdapter.items = item.items
            expandableListTitle.text = context.getString(item.title)
            expandableList.visibleIf { item.isExpanded }
            if (item.isExpanded) {
                expandableListChevron.setImageResource(R.drawable.expand_animated)
                expandableListChevron.setColorFilter(context.getResolvedColor(R.color.blue_600))
            } else {
                expandableListChevron.setImageResource(R.drawable.collapse_animated)
                expandableListChevron.setColorFilter(context.getResolvedColor(R.color.grey_600))
            }
            if (item.playAnimation) {
                val arrow = expandableListChevron.drawable as Animatable
                arrow.start()
            }

            expandableAgreementRoot.setOnClickListener {
                onExpandableItemClicked.invoke(position)
            }
        }
    }

    private class ItemListAdapter : RecyclerView.Adapter<YapilyInfoItemViewHolder>() {
        var items: List<YapilyPermissionItem.YapilyInfoItem> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): YapilyInfoItemViewHolder =
            YapilyInfoItemViewHolder(
                ItemYapilyInfoBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: YapilyInfoItemViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }
}
