package piuk.blockchain.android.ui.customviews.account

import androidx.recyclerview.widget.DiffUtil
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.NullCryptoAccount


internal class AccountsDiffUtil(private val oldAccounts: List<SelectableAccountItem>, private val newAccounts: List<SelectableAccountItem>) :
    DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldAccounts.size

    override fun getNewListSize(): Int = newAccounts.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldAccounts[oldItemPosition].account.eq(newAccounts[newItemPosition].account)

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = false
}

private fun BlockchainAccount.eq(other: BlockchainAccount): Boolean {

    if (this is NullCryptoAccount || other is NullCryptoAccount)
        return false

    if (this::class == other::class &&
        this.label == other.label &&
        this.hasTheSameAsset(other)
    ) {
        return true
    }
    return false
}

private fun BlockchainAccount.hasTheSameAsset(other: BlockchainAccount): Boolean {
    val thisCryptoAsset = (this as? CryptoAccount)?.asset
    val otherCryptoAsset = (this as? CryptoAccount)?.asset
    if (thisCryptoAsset != null && thisCryptoAsset == otherCryptoAsset)
        return true

    val thisFiatAsset = (this as? FiatAccount)?.fiatCurrency
    val otherFiatAsset = (this as? FiatAccount)?.fiatCurrency
    if (thisFiatAsset != null && thisFiatAsset == otherFiatAsset)
        return true

    return false
}
