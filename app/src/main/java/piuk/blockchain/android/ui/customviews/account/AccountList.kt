package piuk.blockchain.android.ui.customviews.account

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
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

class AccountList @JvmOverloads constructor(
    ctx: Context,
    val attr: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(ctx, attr, defStyle) {

    private val disposables = CompositeDisposable()
    private val uiScheduler = AndroidSchedulers.mainThread()
    private val itemList = mutableListOf<BlockchainAccount>()

    init {
        isVerticalFadingEdgeEnabled = true
        setFadingEdgeLength(resources.getDimension(R.dimen.very_small_margin).toInt())

        layoutManager = LinearLayoutManager(
            context,
            VERTICAL,
            false
        )
    }

    fun initialise(
        source: Single<List<BlockchainAccount>>,
        status: StatusDecorator = {
            DefaultCellDecorator()
        },
        introView: IntroHeaderView? = null
    ) {

        addItemDecoration(
            BlockchainListDividerDecor(context)
        )

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
            onAccountClicked = { onAccountSelected(it) }
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
                    itemList.addAll(it)
                    adapter?.notifyDataSetChanged()

                    if (it.isEmpty()) {
                        onEmptyList()
                    }
                },
                onError = {
                    onLoadError(it)
                }
            )
    }

    var onLoadError: (Throwable) -> Unit = {}
    var onAccountSelected: (BlockchainAccount) -> Unit = {}
    var onEmptyList: () -> Unit = {}
}

private class AccountsDelegateAdapter(
    statusDecorator: StatusDecorator,
    onAccountClicked: (BlockchainAccount) -> Unit
) : DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {

    init {
        with(delegatesManager) {
            addAdapterDelegate(
                CryptoAccountDelegate(
                    statusDecorator,
                    onAccountClicked,
                    compositeDisposable
                )
            )
            addAdapterDelegate(
                FiatAccountDelegate(
                    statusDecorator,
                    onAccountClicked,
                    compositeDisposable
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
    private val compositeDisposable: CompositeDisposable
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is CryptoAccount

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        CryptoSingleAccountViewHolder(compositeDisposable, parent.inflate(R.layout.item_account_select_crypto))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as CryptoSingleAccountViewHolder).bind(
        items[position] as CryptoAccount,
        statusDecorator,
        onAccountClicked
    )
}

private class CryptoSingleAccountViewHolder(
    private val compositeDisposable: CompositeDisposable,
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    internal fun bind(
        account: CryptoAccount,
        statusDecorator: StatusDecorator,
        onAccountClicked: (CryptoAccount) -> Unit
    ) {
        with(itemView) {
            crypto_account.updateAccount(account, onAccountClicked, compositeDisposable, statusDecorator(account))
        }
    }
}

private class FiatAccountDelegate<in T>(
    private val statusDecorator: StatusDecorator,
    private val onAccountClicked: (FiatAccount) -> Unit,
    private val compositeDisposable: CompositeDisposable
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is FiatAccount

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        FiatAccountViewHolder(
            compositeDisposable,
            parent.inflate(R.layout.item_account_select_fiat)
        )

    override fun onBindViewHolder(items: List<T>, position: Int, holder: RecyclerView.ViewHolder) =
        (holder as FiatAccountViewHolder).bind(
            items[position] as FiatAccount,
            statusDecorator,
            onAccountClicked
        )
}

private class FiatAccountViewHolder(
    private val compositeDisposable: CompositeDisposable,
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    internal fun bind(
        account: FiatAccount,
        statusDecorator: StatusDecorator,
        onAccountClicked: (FiatAccount) -> Unit
    ) {
        with(itemView) {
            fiat_container.alpha = 1f
            fiat_account.updateAccount(account, compositeDisposable)

            compositeDisposable += statusDecorator(account).isEnabled()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { isEnabled ->
                    if (isEnabled) {
                        setOnClickListener { onAccountClicked(account) }
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
        items[position] is AllWalletsAccount

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AllWalletsAccountViewHolder(compositeDisposable, parent.inflate(R.layout.item_account_select_group))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as AllWalletsAccountViewHolder).bind(
        items[position] as AllWalletsAccount,
        statusDecorator,
        onAccountClicked
    )
}

private class AllWalletsAccountViewHolder(
    private val compositeDisposable: CompositeDisposable,
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    internal fun bind(
        account: AllWalletsAccount,
        statusDecorator: StatusDecorator,
        onAccountClicked: (BlockchainAccount) -> Unit
    ) {
        with(itemView) {

            account_group.updateAccount(account, compositeDisposable)
            account_group.alpha = 1f

            compositeDisposable += statusDecorator(account).isEnabled().observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { isEnabled ->
                        if (isEnabled) {
                            setOnClickListener { onAccountClicked(account) }
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