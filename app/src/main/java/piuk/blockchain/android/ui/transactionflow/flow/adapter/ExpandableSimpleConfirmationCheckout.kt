package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.TxConfirmation
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.databinding.ItemCheckoutSimpleExpandableInfoBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.flow.ConfirmationPropertyKey
import piuk.blockchain.android.ui.transactionflow.flow.TxConfirmReadOnlyMapperCheckout
import piuk.blockchain.android.util.visibleIf
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor

class ExpandableSimpleConfirmationCheckout(private val mapper: TxConfirmReadOnlyMapperCheckout) :
    AdapterDelegate<TxConfirmationValue> {
    override fun isForViewType(items: List<TxConfirmationValue>, position: Int): Boolean {
        return items[position].confirmation == TxConfirmation.EXPANDABLE_SIMPLE_READ_ONLY
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        ExpandableSimpleConfirmationCheckoutItemViewHolder(
            ItemCheckoutSimpleExpandableInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            mapper
        )

    override fun onBindViewHolder(
        items: List<TxConfirmationValue>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as ExpandableSimpleConfirmationCheckoutItemViewHolder).bind(
        items[position]
    )
}

private class ExpandableSimpleConfirmationCheckoutItemViewHolder(
    val binding: ItemCheckoutSimpleExpandableInfoBinding,
    private val mapper: TxConfirmReadOnlyMapperCheckout
) : RecyclerView.ViewHolder(binding.root) {
    private var isExpanded = false

    init {
        with(binding) {
            expandableItemExpansion.movementMethod = LinkMovementMethod.getInstance()
            expandableItemLabel.setOnClickListener {
                isExpanded = !isExpanded
                expandableItemExpansion.visibleIf { isExpanded }
                updateIcon()
            }
        }
    }

    fun bind(item: TxConfirmationValue) {
        with(binding) {
            mapper.map(item).run {
                expandableItemLabel.text = this[ConfirmationPropertyKey.LABEL] as String
                expandableItemTitle.text = this[ConfirmationPropertyKey.TITLE] as String
                expandableItemExpansion.setText(
                    this[ConfirmationPropertyKey.LINKED_NOTE] as SpannableStringBuilder, TextView.BufferType.SPANNABLE
                )
            }
        }
        updateIcon()
    }

    private fun updateIcon() {
        with(binding) {
            // unique drawables will share a single Drawable.ConstantState object, so we need to call mutate to get an individual config instance
            expandableItemLabel.compoundDrawables[DRAWABLE_END]?.mutate()

            if (isExpanded) {
                expandableItemLabel.compoundDrawables[DRAWABLE_END]?.setTint(
                    expandableItemLabel.context.getResolvedColor(R.color.blue_600)
                )
            } else {
                expandableItemLabel.compoundDrawables[DRAWABLE_END]?.setTint(
                    expandableItemLabel.context.getResolvedColor(R.color.grey_300)
                )
            }
        }
    }

    companion object {
        private const val DRAWABLE_END = 2
    }
}