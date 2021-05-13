package piuk.blockchain.android.simplebuy

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemCheckoutComplexInfoBinding
import piuk.blockchain.android.databinding.ItemCheckoutSimpleExpandableInfoBinding
import piuk.blockchain.android.databinding.ItemCheckoutSimpleInfoBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.util.visibleIf
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor

class CheckoutAdapterDelegate : DelegationAdapter<SimpleBuyCheckoutItem>(AdapterDelegatesManager(), emptyList()) {
    init {
        with(delegatesManager) {
            addAdapterDelegate(SimpleCheckoutItemDelegate())
            addAdapterDelegate(ComplexCheckoutItemDelegate())
            addAdapterDelegate(ExpandableCheckoutItemDelegate())
        }
    }
}

sealed class SimpleBuyCheckoutItem {
    data class SimpleCheckoutItem(val label: String, val title: String, val isImportant: Boolean = false) :
        SimpleBuyCheckoutItem()

    data class ComplexCheckoutItem(val label: String, val title: String, val subtitle: String) :
        SimpleBuyCheckoutItem()

    data class ExpandableCheckoutItem(val label: String, val title: String, val expandableContent: CharSequence) :
        SimpleBuyCheckoutItem()
}

class SimpleCheckoutItemDelegate : AdapterDelegate<SimpleBuyCheckoutItem> {

    override fun isForViewType(items: List<SimpleBuyCheckoutItem>, position: Int): Boolean =
        items[position] is SimpleBuyCheckoutItem.SimpleCheckoutItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        SimpleCheckoutItemViewHolder(
            ItemCheckoutSimpleInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<SimpleBuyCheckoutItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as SimpleCheckoutItemViewHolder).bind(
        items[position] as SimpleBuyCheckoutItem.SimpleCheckoutItem
    )
}

private class SimpleCheckoutItemViewHolder(
    val binding: ItemCheckoutSimpleInfoBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: SimpleBuyCheckoutItem.SimpleCheckoutItem) {
        with(binding) {

            simpleItemTitle.text = item.title
            simpleItemLabel.text = item.label
            if (item.isImportant) {
                simpleItemTitle.setTextAppearance(R.style.Text_Semibold_16)
                simpleItemLabel.setTextAppearance(R.style.Text_Semibold_16)
            } else {
                simpleItemTitle.setTextAppearance(R.style.Text_Standard_14)
                simpleItemLabel.setTextAppearance(R.style.Text_Standard_14)
            }
        }
    }
}

class ComplexCheckoutItemDelegate : AdapterDelegate<SimpleBuyCheckoutItem> {

    override fun isForViewType(items: List<SimpleBuyCheckoutItem>, position: Int): Boolean =
        items[position] is SimpleBuyCheckoutItem.ComplexCheckoutItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        ComplexCheckoutItemItemViewHolder(
            ItemCheckoutComplexInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<SimpleBuyCheckoutItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as ComplexCheckoutItemItemViewHolder).bind(
        items[position] as SimpleBuyCheckoutItem.ComplexCheckoutItem
    )
}

private class ComplexCheckoutItemItemViewHolder(
    val binding: ItemCheckoutComplexInfoBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: SimpleBuyCheckoutItem.ComplexCheckoutItem) {
        with(binding) {
            complexItemTitle.text = item.label
            complexItemLabel.text = item.title
            complexItemSubtitle.text = item.subtitle
        }
    }
}

class ExpandableCheckoutItemDelegate : AdapterDelegate<SimpleBuyCheckoutItem> {
    override fun isForViewType(items: List<SimpleBuyCheckoutItem>, position: Int): Boolean =
        items[position] is SimpleBuyCheckoutItem.ExpandableCheckoutItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        ExpandableCheckoutItemViewHolder(
            ItemCheckoutSimpleExpandableInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<SimpleBuyCheckoutItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as ExpandableCheckoutItemViewHolder).bind(
        items[position] as SimpleBuyCheckoutItem.ExpandableCheckoutItem
    )
}

private class ExpandableCheckoutItemViewHolder(
    val binding: ItemCheckoutSimpleExpandableInfoBinding
) : RecyclerView.ViewHolder(binding.root) {
    private var isExpanded = false

    init {
        with(binding) {
            expandableItemExpansion.movementMethod = LinkMovementMethod.getInstance()
            expandableItemTitle.setOnClickListener {
                isExpanded = !isExpanded
                expandableItemExpansion.visibleIf { isExpanded }
                if (isExpanded) {
                    expandableItemTitle.compoundDrawables[DRAWABLE_END_POSITION].setTint(
                        expandableItemTitle.context.getResolvedColor(R.color.blue_600)
                    )
                } else {
                    expandableItemTitle.compoundDrawables[DRAWABLE_END_POSITION].setTint(
                        expandableItemTitle.context.getResolvedColor(R.color.grey_300)
                    )
                }
            }
        }
    }

    fun bind(item: SimpleBuyCheckoutItem.ExpandableCheckoutItem) {
        with(binding) {
            expandableItemLabel.text = item.label
            expandableItemTitle.text = item.title
            expandableItemExpansion.text = item.expandableContent
        }
    }

    companion object {
        const val DRAWABLE_END_POSITION = 2
    }
}
