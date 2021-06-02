package piuk.blockchain.android.ui.dashboard.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.databinding.ItemDashboardFundsBinding
import piuk.blockchain.android.databinding.ItemDashboardFundsBorderedBinding
import piuk.blockchain.android.databinding.ItemDashboardFundsParentBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.FiatAssetState
import piuk.blockchain.android.ui.dashboard.FiatBalanceInfo
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visibleIf

class FundsCardDelegate<in T>(
    private val selectedFiat: String,
    private val onFundsItemClicked: (FiatAccount) -> Unit
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is FiatAssetState

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = ItemDashboardFundsParentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FundsCardViewHolder(
            binding,
            ItemDashboardFundsBinding.inflate(LayoutInflater.from(parent.context), binding.root, true),
            onFundsItemClicked,
            selectedFiat
        )
    }

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as FundsCardViewHolder).bind(items[position] as FiatAssetState)
}

private class FundsCardViewHolder(
    private val binding: ItemDashboardFundsParentBinding,
    private val singleLayoutBinding: ItemDashboardFundsBinding,
    private val onFundsItemClicked: (FiatAccount) -> Unit,
    private val selectedFiat: String
) : RecyclerView.ViewHolder(binding.root) {
    private val multipleFundsAdapter: MultipleFundsAdapter by lazy {
        MultipleFundsAdapter(onFundsItemClicked, selectedFiat)
    }

    fun bind(funds: FiatAssetState) {
        if (funds.fiatAccounts.size == 1) {
            showSingleAsset(funds.fiatAccounts[0])
        } else {
            with(binding) {
                fundsSingleItem.gone()
                fundsList.apply {
                    layoutManager =
                        LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
                    adapter = multipleFundsAdapter
                }
                multipleFundsAdapter.items = funds.fiatAccounts
            }
        }
    }

    private fun showSingleAsset(assetInfo: FiatBalanceInfo) {
        val ticker = assetInfo.account.fiatCurrency
        singleLayoutBinding.apply {
            fundsUserFiatBalance.visibleIf { selectedFiat != ticker }
            fundsUserFiatBalance.text = assetInfo.balance.toStringWithSymbol()
            binding.fundsList.gone()
            binding.fundsSingleItem.setOnClickListener {
                onFundsItemClicked(assetInfo.account)
            }
            fundsTitle.setStringFromTicker(context, ticker)
            fundsFiatTicker.text = ticker
            fundsBalance.text = if (selectedFiat == ticker) {
                assetInfo.balance.toStringWithSymbol()
            } else {
                assetInfo.userFiat.toStringWithSymbol()
            }
            fundsIcon.setIcon(ticker)
        }
    }
}

private class MultipleFundsAdapter(
    private val onFundsItemClicked: (FiatAccount) -> Unit,
    private val selectedFiat: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = listOf<FiatBalanceInfo>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        SingleFundsViewHolder(
            ItemDashboardFundsBorderedBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onFundsItemClicked, selectedFiat
        )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) = (holder as SingleFundsViewHolder).bind(items[position])

    private class SingleFundsViewHolder(
        private val binding: ItemDashboardFundsBorderedBinding,
        private val onFundsItemClicked: (FiatAccount) -> Unit,
        private val selectedFiat: String
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(assetInfo: FiatBalanceInfo) {
            val ticker = assetInfo.account.fiatCurrency
            binding.apply {
                borderedFundsBalanceOtherFiat.visibleIf { selectedFiat != ticker }
                borderedFundsBalanceOtherFiat.text = assetInfo.balance.toStringWithSymbol()

                borderedFundsParent.setOnClickListener {
                    onFundsItemClicked(assetInfo.account)
                }
                borderedFundsTitle.setStringFromTicker(context, ticker)
                borderedFundsFiatTicker.text = ticker
                borderedFundsBalance.text = if (selectedFiat == ticker) {
                    assetInfo.balance.toStringWithSymbol()
                } else {
                    assetInfo.userFiat.toStringWithSymbol()
                }
                borderedFundsIcon.setIcon(ticker)
            }
        }
    }
}

private fun TextView.setStringFromTicker(context: Context, ticker: String) {
    text = context.getString(
        when (ticker) {
            "EUR" -> R.string.euros
            "GBP" -> R.string.pounds
            "USD" -> R.string.us_dollars
            else -> R.string.empty
        }
    )
}
