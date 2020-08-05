package piuk.blockchain.android.ui.dashboard.assetdetails

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
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
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
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
    private val uiScheduler = AndroidSchedulers.mainThread()

    private val prefs: CurrencyPrefs by scopedInject()
    private val exchangeRates: ExchangeRateDataManager by scopedInject()
    private val coincore: Coincore by scopedInject()
    private val labels: DefaultLabels by scopedInject()
    private val model: AssetDetailsModel by scopedInject()

    private val itemAdapter: AssetActionAdapter by lazy {
        AssetActionAdapter()
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
        val firstAccount = account.selectFirstAccount()
        return when (accountType) {
            AssetFilter.Custodial -> {
                showAndMapCustodialUi(view, firstAccount, account)
            }
            AssetFilter.Interest -> {
                showAndMapInterestUi(view, firstAccount, account)
            }
            AssetFilter.NonCustodial -> {
                showAndMapNonCustodialUi(view, firstAccount, account)
            }
            else -> {
                throw IllegalStateException("AssetActions un-catered account type: $account")
            }
        }
    }

    private fun showAndMapNonCustodialUi(
        view: View,
        firstAccount: CryptoAccount,
        account: BlockchainAccount
    ): List<AssetActionItem> {
        view.asset_actions_acc_icon.gone()

        showTextAndAssetIcon(view, getString(firstAccount.asset.assetName()),
            labels.getDefaultNonCustodialWalletLabel(firstAccount.asset),
            firstAccount.asset.drawableResFilled())

        return account.actions.map {
            mapAction(it, firstAccount.asset)
        }
    }

    private fun showAndMapInterestUi(
        view: View,
        firstAccount: CryptoAccount,
        account: BlockchainAccount
    ): List<AssetActionItem> {
        view.asset_actions_acc_icon.setImageResource(
            R.drawable.ic_account_badge_interest)

        disposables += coincore[firstAccount.asset].interestRate().observeOn(uiScheduler)
            .subscribeBy(
                onSuccess = {
                    showTextAndAssetIcon(view,
                        labels.getDefaultInterestWalletLabel(firstAccount.asset),
                        getString(R.string.dashboard_asset_balance_interest, it),
                        firstAccount.asset.drawableResFilled()
                    )
                },
                onError = {
                    showTextAndAssetIcon(view,
                        labels.getDefaultInterestWalletLabel(firstAccount.asset),
                        getString(R.string.dashboard_asset_actions_interest_dsc_failed),
                        firstAccount.asset.drawableResFilled()
                    )
                    Timber.e("AssetActions error loading Interest rate: $it")
                }
            )

        return account.actions.map {
            mapAction(it, firstAccount.asset)
        }
    }

    private fun showAndMapCustodialUi(
        view: View,
        firstAccount: CryptoAccount,
        account: BlockchainAccount
    ): List<AssetActionItem> {
        view.asset_actions_acc_icon.setImageResource(
            R.drawable.ic_account_badge_custodial)

        showTextAndAssetIcon(view, getString(firstAccount.asset.assetName()),
            labels.getDefaultCustodialWalletLabel(firstAccount.asset),
            firstAccount.asset.drawableResFilled())

        return account.actions.map {
            mapAction(it, firstAccount.asset)
        }
    }

    private fun showTextAndAssetIcon(
        view: View,
        title: String,
        subtitle: String,
        @DrawableRes icon: Int
    ) {
        view.asset_actions_title.text = title
        view.asset_actions_details.text = subtitle
        view.asset_actions_asset_icon.setImageResource(icon)
    }

    private fun mapAction(action: AssetAction, asset: CryptoCurrency): AssetActionItem =
        when (action) {
            AssetAction.ViewActivity ->
                AssetActionItem(getString(R.string.activities_title),
                    R.drawable.ic_tx_activity_clock,
                    getString(R.string.fiat_funds_detail_activity_details), asset) {
                    model.process(HandleActionIntent(AssetDetailsAction.ACTIVITY))
                    dismiss()
                }
            AssetAction.Send ->
                AssetActionItem(getString(R.string.common_send), R.drawable.ic_tx_sent,
                    getString(R.string.dashboard_asset_actions_send_dsc, asset.displayTicker),
                    asset) {
                    model.process(HandleActionIntent(AssetDetailsAction.SEND))
                    dismiss()
                }
            AssetAction.NewSend ->
                AssetActionItem(getString(R.string.common_send), R.drawable.ic_tx_sent,
                    getString(R.string.dashboard_asset_actions_send_dsc,
                        asset.displayTicker), asset) {
                    model.process(HandleActionIntent(AssetDetailsAction.SEND))
                    // TODO do we want this to continue as one flow with send?
                    dismiss()
                }
            AssetAction.Receive ->
                AssetActionItem(getString(R.string.common_receive), R.drawable.ic_tx_receive,
                    getString(R.string.dashboard_asset_actions_receive_dsc,
                        asset.displayTicker), asset) {
                    model.process(HandleActionIntent(AssetDetailsAction.RECEIVE))
                    dismiss()
                }
            AssetAction.Swap -> AssetActionItem(getString(R.string.common_swap),
                R.drawable.ic_tx_swap,
                getString(R.string.dashboard_asset_actions_swap_dsc, asset.displayTicker),
                asset) {
                model.process(HandleActionIntent(AssetDetailsAction.SWAP))
                dismiss()
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
                disposables += coincore[asset].accountGroup(AssetFilter.NonCustodial).subscribeBy {
                    if (it.accounts.size > 1) {
                        model.process(SelectSendingAccount)
                    } else {
                        // TODO launch send flow with pre-selected accounts
                    }
                }
            }
        }

    companion object {
        fun newInstance(
            blockchainAccount: BlockchainAccount,
            assetFilter: AssetFilter
        ): AssetActionsSheet {
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