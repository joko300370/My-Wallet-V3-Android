package piuk.blockchain.android.ui.dashboard.assetdetails

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
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
        onAccountSelected: (BlockchainAccount, AssetFilter) -> Unit
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

            rootView.setOnClickListener {
                onAccountSelected(item.account, item.assetFilter)
            }

            when (item.assetFilter) {
                AssetFilter.NonCustodial -> asset_account_icon.gone()
                AssetFilter.Interest -> {
                    asset_account_icon.visible()
                    asset_account_icon.setImageResource(
                        R.drawable.ic_account_badge_interest)
                }
                AssetFilter.Custodial -> {
                    asset_account_icon.visible()
                    asset_account_icon.setImageResource(
                        R.drawable.ic_account_badge_custodial)
                }
                AssetFilter.All -> asset_account_icon.gone()
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

internal class AssetDetailAdapter(
    private val itemList: List<AssetDetailItem>,
    private val onAccountSelected: (BlockchainAccount, AssetFilter) -> Unit,
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
            holder.bind(itemList[position], onAccountSelected)
        } else {
            (holder as LabelViewHolder).bind(token)
        }
    }

    private val TYPE_CRYPTO = 0
    private val TYPE_LABEL = 1
}
