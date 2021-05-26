package piuk.blockchain.android.ui.dashboard.assetdetails.delegates

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.nabu.models.data.RecurringBuyState
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.databinding.ViewAccountRecurringBuyOverviewBinding
import piuk.blockchain.android.simplebuy.toHumanReadableRecurringBuy
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsItem
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.setAssetIconColours
import piuk.blockchain.android.util.toFormattedDate
import piuk.blockchain.android.util.visibleIf

class RecurringBuyItemDelegate(
    private val onRecurringBuyClicked: (RecurringBuy) -> Unit,
    private val assetResources: AssetResources
) : AdapterDelegate<AssetDetailsItem> {
    override fun isForViewType(items: List<AssetDetailsItem>, position: Int): Boolean =
        items[position] is AssetDetailsItem.RecurringBuyInfo

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        RecurringBuyViewHolder(
            ViewAccountRecurringBuyOverviewBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onRecurringBuyClicked,
            assetResources
        )

    override fun onBindViewHolder(
        items: List<AssetDetailsItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as RecurringBuyViewHolder).bind(
        items[position] as AssetDetailsItem.RecurringBuyInfo,
        items.indexOfFirst { it is AssetDetailsItem.RecurringBuyInfo } == position
    )
}

private class RecurringBuyViewHolder(
    private val binding: ViewAccountRecurringBuyOverviewBinding,
    private val onRecurringBuyClicked: (RecurringBuy) -> Unit,
    private val assetResources: AssetResources
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: AssetDetailsItem.RecurringBuyInfo, isFirstItemOfCategory: Boolean) {
        with(binding) {
            rbHeaderGroup.visibleIf { isFirstItemOfCategory }

            rbIcon.setAssetIconColours(
                assetResources.assetTint(item.recurringBuy.asset),
                assetResources.assetFilter(item.recurringBuy.asset)
            )

            rbTitle.text = context.getString(
                R.string.dashboard_recurring_buy_item_title, item.recurringBuy.amount.toStringWithSymbol(),
                item.recurringBuy.asset.displayTicker,
                item.recurringBuy.recurringBuyFrequency.toHumanReadableRecurringBuy(context)
            )

            rbLabel.text = if (item.recurringBuy.state == RecurringBuyState.ACTIVE) {
                context.getString(
                    R.string.dashboard_recurring_buy_item_label, item.recurringBuy.nextPaymentDate.toFormattedDate()
                )
            } else {
                // TODO verify state with BE in next story
                context.getString(R.string.dashboard_recurring_buy_item_label_error)
            }
        }

        binding.root.setOnClickListener { onRecurringBuyClicked(item.recurringBuy) }
    }
}