package piuk.blockchain.android.ui.customviews.account

import androidx.recyclerview.widget.DiffUtil
import piuk.blockchain.android.coincore.isTheSameWith


internal class AccountsDiffUtil(private val oldAccounts: List<SelectableAccountItem>, private val newAccounts: List<SelectableAccountItem>) :
    DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldAccounts.size

    override fun getNewListSize(): Int = newAccounts.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldAccounts[oldItemPosition].account.isTheSameWith(newAccounts[newItemPosition].account)

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = false
}