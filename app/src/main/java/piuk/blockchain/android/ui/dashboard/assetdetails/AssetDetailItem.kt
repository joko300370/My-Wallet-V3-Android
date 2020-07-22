package piuk.blockchain.android.ui.dashboard.assetdetails

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.MenuCompat
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.extensions.exhaustive
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.CustodialBalanceClicked
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import kotlinx.android.synthetic.main.dialog_dashboard_asset_detail_item.view.*
import kotlinx.android.synthetic.main.dialog_dashboard_asset_label_item.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.android.util.assetName
import piuk.blockchain.android.util.setCoinIcon
import piuk.blockchain.androidcoreui.utils.extensions.context
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.setOnClickListenerDebounced
import piuk.blockchain.androidcoreui.utils.extensions.visible

data class AssetDetailItem(
    val assetFilter: AssetFilter,
    val account: BlockchainAccount,
    val balance: Money,
    val fiatBalance: Money,
    val actions: Set<AssetAction>,
    val interestRate: Double
)

class AssetDetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(
        item: AssetDetailItem,
        onActionSelected: AssetActionHandler,
        analytics: Analytics
    ) {
        with(itemView) {
            val asset = getAsset(item.account)

            icon.setCoinIcon(asset)
            asset_name.text = resources.getString(asset.assetName())

            status_date.text = when (item.assetFilter) {
                AssetFilter.All -> resources.getString(R.string.dashboard_asset_balance_total)
                AssetFilter.NonCustodial -> resources.getString(
                    R.string.dashboard_asset_balance_wallet
                )
                AssetFilter.Custodial -> resources.getString(
                    R.string.dashboard_asset_balance_custodial
                )
                AssetFilter.Interest -> resources.getString(
                    R.string.dashboard_asset_balance_interest, item.interestRate
                )
            }

            if (item.actions.isEmpty()) {
                action_menu.invisible()
            } else {
                action_menu.visible()
                setOnClickListenerDebounced { doShowMenu(item, onActionSelected, analytics) }
            }

            asset_spend_locked.goneIf {
                item.assetFilter == AssetFilter.NonCustodial || item.assetFilter == AssetFilter.Interest
            }

            asset_balance_crypto.text = item.balance.toStringWithSymbol()
            asset_balance_fiat.text = item.fiatBalance.toStringWithSymbol()
        }
    }

    private fun getAsset(account: BlockchainAccount): CryptoCurrency =
        when (account) {
            is CryptoAccount -> account.asset
            is AccountGroup -> account.accounts.filterIsInstance<CryptoAccount>()
                .firstOrNull()?.asset
            else -> null
        } ?: throw IllegalStateException("Unsupported account type")

    private fun doShowMenu(
        detailItem: AssetDetailItem,
        onActionSelected: AssetActionHandler,
        analytics: Analytics
    ) {
        if (detailItem.account is CryptoAccount) {
            if (detailItem.assetFilter == AssetFilter.Custodial) {
                val crypto = detailItem.account.asset
                analytics.logEvent(CustodialBalanceClicked(crypto))
            }
        }

        PopupMenu(itemView.context, itemView.action_menu).apply {
            menuInflater.inflate(R.menu.menu_asset_actions, menu)

            // enable available actions
            detailItem.actions.forEach {
                menu.findItem(mapActionToMenuItem(it))?.isVisible = true
            }

            MenuCompat.setGroupDividerEnabled(menu, true)

            setOnMenuItemClickListener {
                val account = detailItem.account
                val action = mapMenuItemToAction(it.itemId, account)

                onActionSelected(action, account)
                true
            }

            setOnDismissListener {
                itemView.setBackgroundColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.white
                    )
                )
            }

            gravity = Gravity.END
            show()
        }

        itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.grey_000))
    }

    private fun mapMenuItemToAction(menuId: Int, account: BlockchainAccount): AssetAction =
        when (menuId) {
            R.id.action_activity -> AssetAction.ViewActivity
            R.id.action_send -> if (account.actions.contains(AssetAction.Send))
                AssetAction.Send
                    else
                AssetAction.NewSend
            R.id.action_receive -> AssetAction.Receive
            R.id.action_swap -> AssetAction.Swap
            else -> throw IllegalArgumentException("id maps to unknown action")
        }

    private fun mapActionToMenuItem(action: AssetAction): Int =
        when (action) {
            AssetAction.ViewActivity -> R.id.action_activity
            AssetAction.Send,
            AssetAction.NewSend -> R.id.action_send
            AssetAction.Receive -> R.id.action_receive
            AssetAction.Swap -> R.id.action_swap
        }.exhaustive
}

class LabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(token: CryptoAsset) {
        itemView.asset_label_description.text = when (token.asset) {
            CryptoCurrency.ALGO -> context.getString(R.string.algorand_asset_label)
            CryptoCurrency.USDT -> context.getString(R.string.usdt_asset_label)
            else -> ""
        }
    }
}

typealias AssetActionHandler = (action: AssetAction, account: BlockchainAccount) -> Unit

internal class AssetDetailAdapter(
    private val itemList: List<AssetDetailItem>,
    private val onActionSelected: AssetActionHandler,
    private val analytics: Analytics,
    private val showBanner: Boolean,
    private val token: CryptoAsset
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == TYPE_CRYPTO) {
            AssetDetailViewHolder(parent.inflate(R.layout.dialog_dashboard_asset_detail_item))
        } else {
            LabelViewHolder(parent.inflate(R.layout.dialog_dashboard_asset_label_item))
        }

    override fun getItemCount(): Int = if (showBanner) itemList.size + 1 else itemList.size

    override fun getItemViewType(position: Int): Int =
        if (showBanner) {
            if (position >= itemList.size) {
                TYPE_LABEL
            } else {
                TYPE_CRYPTO
            }
        } else {
            TYPE_CRYPTO
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AssetDetailViewHolder) {
            holder.bind(itemList[position], onActionSelected, analytics)
        } else {
            (holder as LabelViewHolder).bind(token)
        }
    }

    private val TYPE_CRYPTO = 0
    private val TYPE_LABEL = 1
}
