package piuk.blockchain.android.ui.chooser

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import piuk.blockchain.android.ui.account.ItemAccount
import java.util.Collections

class WalletAccountHelper(
    private val payloadManager: PayloadManager
) {
    /**
     * Returns a list of [ItemAccount] objects containing only HD accounts.
     *
     * @return Returns a list of [ItemAccount] objects
     */
    fun getHdAccounts(): List<ItemAccount> {
        val list = payloadManager.payload?.hdWallets?.get(0)?.accounts
            ?: Collections.emptyList<Account>()
        // Skip archived account
        return list.filterNot { it.isArchived }
            .map {
                ItemAccount(
                    label = it.label,
                    balance = CryptoValue.fromMinor(CryptoCurrency.BTC, payloadManager.getAddressBalance(it.xpub)),
                    accountObject = it,
                    address = it.xpub,
                    type = ItemAccount.TYPE.SINGLE_ACCOUNT
                )
            }
    }

    /**
     * Returns a list of [ItemAccount] objects containing only [LegacyAddress] objects.
     *
     * @return Returns a list of [ItemAccount] objects
     */
    fun getLegacyBtcAddresses(): List<ItemAccount> {
        val list = payloadManager.payload?.legacyAddressList
            ?: Collections.emptyList<LegacyAddress>()
        // Skip archived address
        return list.filterNot { it.tag == LegacyAddress.ARCHIVED_ADDRESS }
            .map {
                ItemAccount(
                    label = makeLabel(it),
                    balance = CryptoValue.fromMinor(CryptoCurrency.BTC, getAddressAbsoluteBalance(it)),
                    tag = "",
                    accountObject = it,
                    address = it.address
                )
            }
    }

    // If address has no label, we'll display address
    private fun makeLabel(address: LegacyAddress): String {
        var labelOrAddress: String? = address.label
        if (labelOrAddress == null || labelOrAddress.trim { it <= ' ' }.isEmpty()) {
            labelOrAddress = address.address
        }
        return labelOrAddress ?: ""
    }

    private fun getAddressAbsoluteBalance(legacyAddress: LegacyAddress) =
        payloadManager.getAddressBalance(legacyAddress.address)
}
