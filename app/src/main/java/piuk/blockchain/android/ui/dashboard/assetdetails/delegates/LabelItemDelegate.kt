package piuk.blockchain.android.ui.dashboard.assetdetails.delegates

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.android.databinding.DialogDashboardAssetLabelItemBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsItem
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.gone

class LabelItemDelegate(private val token: CryptoAsset) : AdapterDelegate<AssetDetailsItem> {
    override fun isForViewType(items: List<AssetDetailsItem>, position: Int): Boolean =
        items[position] is AssetDetailsItem.AssetLabel

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        LabelViewHolder(
            DialogDashboardAssetLabelItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<AssetDetailsItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as LabelViewHolder).bind(token)
}

private class LabelViewHolder(
    val binding: DialogDashboardAssetLabelItemBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(token: CryptoAsset) {
        with(binding) {
            when (token.asset) {
                CryptoCurrency.ALGO -> assetLabelDescription.text = context.getString(R.string.algorand_asset_label)
                CryptoCurrency.DOT -> assetLabelDescription.text = context.getString(R.string.polkadot_asset_label)
                else -> root.gone()
            }
        }
    }
}