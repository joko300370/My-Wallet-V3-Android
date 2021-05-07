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
import piuk.blockchain.android.databinding.ItemCheckoutComplexExpandableInfoBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.flow.ConfirmationPropertyKey
import piuk.blockchain.android.ui.transactionflow.flow.TxConfirmReadOnlyMapperNewCheckout
import piuk.blockchain.android.util.visibleIf
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor

class ExpandableComplexConfirmationCheckout(private val mapper: TxConfirmReadOnlyMapperNewCheckout) :
    AdapterDelegate<TxConfirmationValue> {
    override fun isForViewType(items: List<TxConfirmationValue>, position: Int): Boolean {
        return items[position].confirmation == TxConfirmation.EXPANDABLE_COMPLEX_READ_ONLY
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        ExpandableComplexConfirmationCheckoutItemViewHolder(
            ItemCheckoutComplexExpandableInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            mapper
        )

    override fun onBindViewHolder(
        items: List<TxConfirmationValue>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as ExpandableComplexConfirmationCheckoutItemViewHolder).bind(
        items[position]
    )
}

private class ExpandableComplexConfirmationCheckoutItemViewHolder(
    val binding: ItemCheckoutComplexExpandableInfoBinding,
    private val mapper: TxConfirmReadOnlyMapperNewCheckout
) : RecyclerView.ViewHolder(binding.root) {
    private var isExpanded = false

    init {
        with(binding) {
            expandableComplexItemExpansion.movementMethod = LinkMovementMethod.getInstance()
            expandableComplexItemLabel.setOnClickListener {
                isExpanded = !isExpanded
                expandableComplexItemExpansion.visibleIf { isExpanded }
                updateIcon()
            }
        }
    }

    fun bind(item: TxConfirmationValue) {
        with(binding) {
            mapper.map(item).run {
                expandableComplexItemLabel.text = this[ConfirmationPropertyKey.LABEL] as String
                expandableComplexItemTitle.text = this[ConfirmationPropertyKey.TITLE] as String
                expandableComplexItemSubtitle.text = this[ConfirmationPropertyKey.SUBTITLE] as String
                expandableComplexItemExpansion.setText(
                    this[ConfirmationPropertyKey.LINKED_NOTE] as SpannableStringBuilder, TextView.BufferType.SPANNABLE
                )
            }
        }
        updateIcon()
    }

    private fun updateIcon() {
        with(binding) {
            // unique drawables will share a single Drawable.ConstantState object, so we need to call mutate to get an individual config instance
            expandableComplexItemLabel.compoundDrawables[DRAWABLE_END]?.mutate()

            if (isExpanded) {
                expandableComplexItemLabel.compoundDrawables[DRAWABLE_END]?.setTint(
                    expandableComplexItemLabel.context.getResolvedColor(R.color.blue_600)
                )
            } else {
                expandableComplexItemLabel.compoundDrawables[DRAWABLE_END]?.setTint(
                    expandableComplexItemLabel.context.getResolvedColor(R.color.grey_300)
                )
            }
        }
    }

    companion object {
        private const val DRAWABLE_END = 2
    }
}