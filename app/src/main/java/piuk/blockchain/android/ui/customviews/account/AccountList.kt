package piuk.blockchain.android.ui.customviews.account

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.item_account_select_crypto.view.*
import kotlinx.android.synthetic.main.item_account_select_fiat.view.*
import kotlinx.android.synthetic.main.item_account_select_group.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.accounts.CellDecorator
import piuk.blockchain.android.accounts.DefaultCellDecorator
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.impl.AllWalletsAccount
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.customviews.IntroHeaderView
import piuk.blockchain.androidcoreui.utils.extensions.inflate

typealias StatusDecorator = (BlockchainAccount) -> CellDecorator

private data class SelectableAccountItem(
    val account: BlockchainAccount,
    var isSelected: Boolean
)

class AccountList @JvmOverloads constructor(
    ctx: Context,
    val attr: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(ctx, attr, defStyle) {

    private val disposables = CompositeDisposable()
    private val uiScheduler = AndroidSchedulers.mainThread()
    private val itemList = mutableListOf<SelectableAccountItem>()
    private var lastSelectedAccount: BlockchainAccount? = null

    init {
        setBackgroundColor(Color.WHITE)
        setFadingEdgeLength(resources.getDimensionPixelSize(R.dimen.size_small))
        isVerticalFadingEdgeEnabled = true
        layoutManager = LinearLayoutManager(
            context,
            VERTICAL,
            false
        )
        addItemDecoration(
            BlockchainListDividerDecor(context)
        )
    }

    fun initialise(
        source: Single<List<BlockchainAccount>>,
        status: StatusDecorator = {
            DefaultCellDecorator()
        },
        introView: IntroHeaderView? = null,
        shouldShowSelectionStatus: Boolean = false
    ) {

        introView?.let {
            addItemDecoration(
                HeaderDecoration.with(context)
                    .parallax(0.5f)
                    .setView(it)
                    .build()
            )
        }

        val theAdapter = AccountsDelegateAdapter(
            statusDecorator = status,
            onAccountClicked = { onAccountSelected(it) },
            showSelectionStatus = shouldShowSelectionStatus
        )
        adapter = theAdapter
        theAdapter.items = itemList

        loadItems(source)
    }

    fun loadItems(source: Single<List<BlockchainAccount>>) {
        disposables += source
            .observeOn(uiScheduler)
            .subscribeBy(
                onSuccess = {
                    itemList.clear()
                    itemList.addAll(it.map { account ->
                        SelectableAccountItem(account, false)
                    })
                    adapter?.notifyDataSetChanged()

                    if (it.isEmpty()) {
                        onEmptyList()
                    } else {
                        onListLoaded()
                    }

                    lastSelectedAccount?.let {
                        updatedSelectedAccount(it)
                        lastSelectedAccount = null
                    }
                },
                onError = {
                    onLoadError(it)
                }
            )
    }

    fun updatedSelectedAccount(selectedAccount: BlockchainAccount) {
        if (itemList.isNotEmpty()) {
            itemList.map { selectableAccount ->
                selectableAccount.isSelected = selectableAccount.account == selectedAccount
            }

            adapter?.notifyDataSetChanged()
        } else {
            // if list is empty, we're in a race condition between loading and selecting, so store value and check
            // it once items loaded
            lastSelectedAccount = selectedAccount
        }
    }

    fun clearSelectedAccount() {
        itemList.map {
            it.isSelected = false
        }
        adapter?.notifyDataSetChanged()
    }

    var onLoadError: (Throwable) -> Unit = {}
    var onAccountSelected: (BlockchainAccount) -> Unit = {}
    var onEmptyList: () -> Unit = {}
    var onListLoaded: () -> Unit = {}
}

private class AccountsDelegateAdapter(
    statusDecorator: StatusDecorator,
    onAccountClicked: (BlockchainAccount) -> Unit,
    showSelectionStatus: Boolean
) : DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {
    init {
        with(delegatesManager) {
            addAdapterDelegate(
                CryptoAccountDelegate(
                    statusDecorator,
                    onAccountClicked,
                    compositeDisposable,
                    showSelectionStatus
                )
            )
            addAdapterDelegate(
                FiatAccountDelegate(
                    statusDecorator,
                    onAccountClicked,
                    compositeDisposable,
                    showSelectionStatus
                )
            )
            addAdapterDelegate(
                AllWalletsAccountDelegate(
                    statusDecorator,
                    onAccountClicked,
                    compositeDisposable
                )
            )
        }
    }
}

private class CryptoAccountDelegate<in T>(
    private val statusDecorator: StatusDecorator,
    private val onAccountClicked: (CryptoAccount) -> Unit,
    private val compositeDisposable: CompositeDisposable,
    private val showSelectionStatus: Boolean
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        (items[position] as SelectableAccountItem).account is CryptoAccount

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        CryptoSingleAccountViewHolder(compositeDisposable,
            showSelectionStatus,
            parent.inflate(R.layout.item_account_select_crypto))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as CryptoSingleAccountViewHolder).bind(
        items[position] as SelectableAccountItem,
        statusDecorator,
        onAccountClicked
    )
}

private class CryptoSingleAccountViewHolder(
    private val compositeDisposable: CompositeDisposable,
    private val showSelectionStatus: Boolean,
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    fun bind(
        selectableAccountItem: SelectableAccountItem,
        statusDecorator: StatusDecorator,
        onAccountClicked: (CryptoAccount) -> Unit
    ) {
        with(itemView) {
            if (showSelectionStatus) {
                if (selectableAccountItem.isSelected) {
                    crypto_account_parent.background = ContextCompat.getDrawable(context, R.drawable.item_selected_bkgd)
                } else {
                    crypto_account_parent.background = null
                }
            }
            crypto_account.updateAccount(selectableAccountItem.account as CryptoAccount, onAccountClicked,
                compositeDisposable,
                statusDecorator(selectableAccountItem.account))
        }
    }
}

private class FiatAccountDelegate<in T>(
    private val statusDecorator: StatusDecorator,
    private val onAccountClicked: (FiatAccount) -> Unit,
    private val compositeDisposable: CompositeDisposable,
    private val showSelectionStatus: Boolean
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        (items[position] as SelectableAccountItem).account is FiatAccount

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        FiatAccountViewHolder(
            compositeDisposable,
            showSelectionStatus,
            parent.inflate(R.layout.item_account_select_fiat)
        )

    override fun onBindViewHolder(items: List<T>, position: Int, holder: RecyclerView.ViewHolder) =
        (holder as FiatAccountViewHolder).bind(
            items[position] as SelectableAccountItem,
            statusDecorator,
            onAccountClicked
        )
}

private class FiatAccountViewHolder(
    private val compositeDisposable: CompositeDisposable,
    private val showSelectionStatus: Boolean,
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    internal fun bind(
        selectableAccountItem: SelectableAccountItem,
        statusDecorator: StatusDecorator,
        onAccountClicked: (FiatAccount) -> Unit
    ) {
        with(itemView) {
            if (showSelectionStatus) {
                if (selectableAccountItem.isSelected) {
                    fiat_container.background = ContextCompat.getDrawable(context, R.drawable.item_selected_bkgd)
                } else {
                    fiat_container.background = null
                }
            }
            fiat_container.alpha = 1f
            fiat_account.updateAccount(selectableAccountItem.account as FiatAccount, compositeDisposable)

            compositeDisposable += statusDecorator(selectableAccountItem.account).isEnabled()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { isEnabled ->
                    if (isEnabled) {
                        setOnClickListener { onAccountClicked(selectableAccountItem.account) }
                        fiat_container.alpha = 1f
                    } else {
                        fiat_container.alpha = .6f
                        setOnClickListener { }
                    }
                }
        }
    }
}

private class AllWalletsAccountDelegate<in T>(
    private val statusDecorator: StatusDecorator,
    private val onAccountClicked: (BlockchainAccount) -> Unit,
    private val compositeDisposable: CompositeDisposable
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        (items[position] as SelectableAccountItem).account is AllWalletsAccount

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AllWalletsAccountViewHolder(compositeDisposable, parent.inflate(R.layout.item_account_select_group))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as AllWalletsAccountViewHolder).bind(
        items[position] as SelectableAccountItem,
        statusDecorator,
        onAccountClicked
    )
}

private class AllWalletsAccountViewHolder(
    private val compositeDisposable: CompositeDisposable,
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    fun bind(
        selectableAccountItem: SelectableAccountItem,
        statusDecorator: StatusDecorator,
        onAccountClicked: (BlockchainAccount) -> Unit
    ) {
        with(itemView) {

            account_group.updateAccount(selectableAccountItem.account as AllWalletsAccount, compositeDisposable)
            account_group.alpha = 1f

            compositeDisposable += statusDecorator(selectableAccountItem.account).isEnabled()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { isEnabled ->
                        if (isEnabled) {
                            setOnClickListener { onAccountClicked(selectableAccountItem.account) }
                            account_group.alpha = 1f
                        } else {
                            account_group.alpha = .6f
                            setOnClickListener { }
                        }
                    }
                )
        }
    }
}