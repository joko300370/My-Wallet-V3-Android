package piuk.blockchain.android.ui.dashboard.assetdetails

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.item_asset_action.view.*
import kotlinx.android.synthetic.main.sheet_asset_actions.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.dashboard.DashboardModel
import piuk.blockchain.android.util.assetFilter
import piuk.blockchain.android.util.assetName
import piuk.blockchain.android.util.assetTint
import piuk.blockchain.android.util.drawableResFilled
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import timber.log.Timber

class AssetActionsSheet : SlidingModalBottomDialog() {
    private lateinit var account: BlockchainAccount
    private lateinit var accountType: AssetFilter
    private val disposables = CompositeDisposable()
    private val prefs: CurrencyPrefs by scopedInject()
    private val exchangeRates: ExchangeRateDataManager by scopedInject()
    private val coincore: Coincore by scopedInject()
    private val labels: DefaultLabels by scopedInject()
    private val model: AssetDetailsModel by scopedInject()
    private val uiScheduler = AndroidSchedulers.mainThread()

    private val itemAdapter: AssetActionAdapter by lazy {
        AssetActionAdapter()
    }

    interface Host : SlidingModalBottomDialog.Host {
        fun launchNewSendFor(account: SingleAccount)
        fun gotoSendFor(account: SingleAccount)
        fun goToReceiveFor(account: SingleAccount)
        fun gotoActivityFor(account: BlockchainAccount)
        fun gotoSwap(account: SingleAccount)
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a AssetActionsSheet.Host")
    }

    override val layoutResource: Int
        get() = R.layout.sheet_asset_actions

    override fun initControls(view: View) {
        disposables += Singles.zip(
            account.balance,
            account.fiatBalance(prefs.selectedFiatCurrency, exchangeRates)
        ).observeOn(uiScheduler).subscribeBy(
            onSuccess = { (balance, fiatBalance) ->
                view.asset_actions_crypto_value.text = balance.toStringWithSymbol()
                view.asset_actions_fiat_value.text = fiatBalance.toStringWithSymbol()
            },
            onError = {
                Timber.e("ActionSheet error loading balances: $it")
            }
        )

        view.asset_actions_list.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
            adapter = itemAdapter
        }

        val actionItems = mapDetailsAndActions(view, account, accountType)
        itemAdapter.itemList = actionItems

        view.asset_actions_back.setOnClickListener {
            model.process(ReturnToPreviousStep)
        }
    }

    private fun mapDetailsAndActions(
        view: View,
        account: BlockchainAccount,
        accountType: AssetFilter
    ): List<AssetActionItem> {
        val firstAccount = selectAccount(account)
        return when (accountType) {
            AssetFilter.Custodial -> {
                view.asset_actions_title.text = getString(firstAccount.asset.assetName())
                view.asset_actions_details.text =
                    labels.getDefaultCustodialWalletLabel(firstAccount.asset)
                view.asset_actions_asset_icon.setImageResource(
                    firstAccount.asset.drawableResFilled())
                view.asset_actions_acc_icon.setImageResource(
                    R.drawable.ic_account_badge_custodial)
                account.actions.map {
                    mapAction(it, firstAccount.asset, firstAccount)
                }
            }
            AssetFilter.Interest -> {
                view.asset_actions_title.text =
                    labels.getDefaultInterestWalletLabel(firstAccount.asset)
                view.asset_actions_asset_icon.setImageResource(
                    firstAccount.asset.drawableResFilled())
                view.asset_actions_acc_icon.setImageResource(
                    R.drawable.ic_account_badge_interest)
                disposables += coincore[firstAccount.asset].interestRate().observeOn(uiScheduler)
                    .subscribeBy(
                        onSuccess = {
                            view.asset_actions_details.text =
                                getString(R.string.dashboard_asset_balance_interest, it)
                        },
                        onError = {
                            Timber.e("AssetActions error loading Interest rate: $it")
                        }
                    )
                account.actions.map {
                    mapAction(it, firstAccount.asset, firstAccount)
                }
            }
            AssetFilter.NonCustodial -> {
                view.asset_actions_acc_icon.gone()
                view.asset_actions_title.text = getString(firstAccount.asset.assetName())
                view.asset_actions_details.text =
                    labels.getDefaultNonCustodialWalletLabel(firstAccount.asset)
                view.asset_actions_asset_icon.setImageResource(
                    firstAccount.asset.drawableResFilled())
                account.actions.map {
                    mapAction(it, firstAccount.asset, firstAccount)
                }
            }
            else -> {
                throw IllegalStateException("AssetActions un-catered account type: $account")
            }
        }
    }

    private fun mapAction(action: AssetAction, asset: CryptoCurrency, account: SingleAccount): AssetActionItem =
        when (action) {
            AssetAction.ViewActivity ->
                AssetActionItem(getString(R.string.activities_title),
                    R.drawable.ic_tx_activity_clock,
                    getString(R.string.fiat_funds_detail_activity_details), asset) {
                    dismiss()
                    host.gotoActivityFor(account)
                }
            AssetAction.Send ->
                AssetActionItem(getString(R.string.common_send), R.drawable.ic_tx_sent,
                    getString(R.string.dashboard_asset_actions_send_dsc, asset.displayTicker),
                    asset) {
                    dismiss()
                    host.gotoSendFor(account)
                }
            AssetAction.NewSend ->
                AssetActionItem(getString(R.string.common_send), R.drawable.ic_tx_sent,
                    getString(R.string.dashboard_asset_actions_send_dsc,
                        asset.displayTicker), asset) {
                    // TODO do we want this to continue as one flow with send?
                    dismiss()
                    host.launchNewSendFor(account)
                }
            AssetAction.Receive ->
                AssetActionItem(getString(R.string.common_receive), R.drawable.ic_tx_receive,
                    getString(R.string.dashboard_asset_actions_receive_dsc,
                        asset.displayTicker), asset) {
                    dismiss()
                    host.goToReceiveFor(account)
                }
            AssetAction.Swap -> AssetActionItem(getString(R.string.common_swap),
                R.drawable.ic_tx_swap,
                getString(R.string.dashboard_asset_actions_swap_dsc, asset.displayTicker),
                asset) {
                dismiss()
                host.gotoSwap(account)
            }
            AssetAction.Summary -> AssetActionItem(
                getString(R.string.dashboard_asset_actions_summary_title),
                R.drawable.ic_tx_interest,
                getString(R.string.dashboard_asset_actions_summary_dsc, asset.networkTicker),
                asset) {
                // TODO in upcoming story
                Timber.e("---- summary clicked")
            }
            AssetAction.Deposit -> AssetActionItem(getString(R.string.common_deposit),
                R.drawable.ic_tx_deposit_arrow,
                getString(R.string.dashboard_asset_actions_deposit_dsc, asset.networkTicker),
                asset) {
                // TODO in upcoming story
                Timber.e("----- deposit clicked")
            }
        }

    private fun selectAccount(account: BlockchainAccount): CryptoAccount {
        val selectedAccount = when (account) {
                is SingleAccount -> account
                is AccountGroup -> account.accounts
                    .firstOrNull { a -> a.isDefault }
                    ?: account.accounts.firstOrNull()
                    ?: throw IllegalStateException("No SingleAccount found")
                else -> throw IllegalStateException("Unknown account base")
            }

        return selectedAccount as CryptoAccount
    }

    companion object {
        fun newInstance(blockchainAccount: BlockchainAccount): AssetActionsSheet {
            return AssetActionsSheet().apply {
                account = blockchainAccount
            }
        }

        fun newInstance(blockchainAccount: BlockchainAccount, assetFilter: AssetFilter): AssetActionsSheet {
            return AssetActionsSheet().apply {
                account = blockchainAccount
                accountType = assetFilter
            }
        }
    }
}

private class AssetActionAdapter : RecyclerView.Adapter<AssetActionAdapter.ActionItemViewHolder>() {
    var itemList: List<AssetActionItem> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionItemViewHolder =
        ActionItemViewHolder(parent.inflate(R.layout.item_asset_action))

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: ActionItemViewHolder, position: Int) =
        holder.bind(itemList[position])

    private class ActionItemViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: AssetActionItem) {
            view.item_action_holder.setOnClickListener {
                item.actionCta()
            }

            view.item_action_icon.setImageResource(item.icon)
            view.item_action_icon.setAssetIconColours(item.asset, view.context)
            view.item_action_title.text = item.title
            view.item_action_label.text = item.description
        }

        private fun ImageView.setAssetIconColours(
            cryptoCurrency: CryptoCurrency,
            context: Context
        ) {
            setBackgroundResource(R.drawable.bkgd_tx_circle)
            background.setTint(ContextCompat.getColor(context, cryptoCurrency.assetTint()))
            setColorFilter(ContextCompat.getColor(context, cryptoCurrency.assetFilter()))
        }
    }
}

private data class AssetActionItem(
    val title: String,
    val icon: Int,
    val description: String,
    val asset: CryptoCurrency,
    val actionCta: () -> Unit
)