package piuk.blockchain.android.ui.addresses.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.databinding.ItemAccountsRowBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.customviews.account.CellDecorator
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible

class AccountDelegate(
    val listener: AccountAdapter.Listener
) : AdapterDelegate<AccountListItem> {

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AccountViewHolder(
            ItemAccountsRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<AccountListItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) {
        val accountViewHolder = holder as AccountViewHolder
        accountViewHolder.bind(items[position] as AccountListItem.Account, listener)
    }

    override fun isForViewType(items: List<AccountListItem>, position: Int) =
        (items[position] is AccountListItem.Account)

    private class AccountViewHolder(
        private val binding: ItemAccountsRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: AccountListItem.Account,
            listener: AccountAdapter.Listener
        ) {
            with(binding) {
                if (item.account.isArchived) {
                    // Show the archived item
                    accountDetails.gone()
                    accountDetailsArchived.visible()
                    accountDetailsArchived.updateAccount(
                        item.account
                    ) { listener.onAccountClicked(it) }
                    accountDetails.contentDescription = ""
                    accountDetailsArchived.contentDescription = item.account.label
                } else {
                    // Show the normal item
                    accountDetailsArchived.gone()
                    accountDetails.visible()
                    accountDetails.updateAccount(
                        item.account,
                        { listener.onAccountClicked(it) },
                        DefaultAccountCellDecorator(item.account)
                    )
                    accountDetailsArchived.contentDescription = ""
                    accountDetails.contentDescription = item.account.label
                }
            }
        }
    }
}

class DefaultAccountCellDecorator(private val account: CryptoAccount) : CellDecorator {
    override fun view(context: Context): Maybe<View> =
        if (account.isDefault) {
            defaultLabel(context)
        } else {
            Maybe.empty()
        }

    private fun defaultLabel(context: Context): Maybe<View> =
        Maybe.just(
            LayoutInflater.from(context)
                .inflate(R.layout.decorator_account_default, null, false)
            )

    override fun isEnabled(): Single<Boolean> = Single.just(true)
}