package piuk.blockchain.android.ui.activity.detail.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.databinding.ItemActivityDetailActionBinding
import piuk.blockchain.android.ui.activity.detail.Action
import piuk.blockchain.android.ui.activity.detail.ActivityDetailsType
import piuk.blockchain.android.ui.adapters.AdapterDelegate

class ActivityDetailActionItemDelegate<in T>(
    private val onActionItemClicked: () -> Unit
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean {
        val item = items[position] as ActivityDetailsType
        return item is Action
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        ActionItemViewHolder(
            ItemActivityDetailActionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as ActionItemViewHolder).bind(
        onActionItemClicked
    )
}

private class ActionItemViewHolder(private val binding: ItemActivityDetailActionBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(actionItemClicked: () -> Unit) {
        binding.activityDetailsAction.setOnClickListener { actionItemClicked() }
    }
}