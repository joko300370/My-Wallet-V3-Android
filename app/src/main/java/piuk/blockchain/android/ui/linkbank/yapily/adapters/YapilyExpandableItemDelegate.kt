package piuk.blockchain.android.ui.linkbank.yapily.adapters

import android.graphics.drawable.Animatable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemYapilyExpandableBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.visibleIf
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor

class YapilyExpandableItemDelegate(
    private val onExpandableItemClicked: (Int) -> Unit
) : AdapterDelegate<YapilyPermissionItem> {
    override fun isForViewType(items: List<YapilyPermissionItem>, position: Int): Boolean =
        items[position] is YapilyPermissionItem.YapilyExpandableItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        YapilyExpandableItemViewHolder(
            ItemYapilyExpandableBinding.inflate(
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
    ) = (holder as YapilyExpandableItemViewHolder).bind(
        items[position] as YapilyPermissionItem.YapilyExpandableItem, position
    )
}

class YapilyExpandableItemViewHolder(
    val binding: ItemYapilyExpandableBinding,
    private val onExpandableItemClicked: (Int) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: YapilyPermissionItem.YapilyExpandableItem, position: Int) {
        setInfo(item.title, item.blurb)
        updateExpandedStateAndAnimation(item.isExpanded, item.playAnimation)
        updateClickListener(position)
    }

    private fun setInfo(@StringRes title: Int, @StringRes blurb: Int) {
        with(binding) {
            expandableTitle.text = context.getString(title)
            expandableBlurb.text = context.getString(blurb)
        }
    }

    private fun updateExpandedStateAndAnimation(isExpanded: Boolean, playAnimation: Boolean) {
        with(binding) {
            expandableBlurb.visibleIf { isExpanded }
            if (isExpanded) {
                expandableChevron.setImageResource(R.drawable.expand_animated)
                expandableChevron.setColorFilter(context.getResolvedColor(R.color.blue_600))
            } else {
                expandableChevron.setImageResource(R.drawable.collapse_animated)
                expandableChevron.setColorFilter(context.getResolvedColor(R.color.grey_600))
            }
            if (playAnimation) {
                val arrow = expandableChevron.drawable as Animatable
                arrow.start()
            }
        }
    }

    private fun updateClickListener(position: Int) {
        binding.expandableRoot.setOnClickListener {
            onExpandableItemClicked.invoke(position)
        }
    }
}
