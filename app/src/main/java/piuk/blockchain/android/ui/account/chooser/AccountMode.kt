package piuk.blockchain.android.ui.account.chooser

import android.os.Parcelable
import info.blockchain.balance.CryptoCurrency
import kotlinx.android.parcel.Parcelize

sealed class AccountMode : Parcelable {
    /**
     * Show all accounts for ShapeShift, ie BTC & BCH HD accounts, Ether
     */
    @Parcelize
    object Exchange : AccountMode()

    /**
     * Show all cryptoCurrency accounts, including HD + legacy addresses
     * If hdOnly = true then show all  accounts without legacy addresses
     * If isSend = true Show all  cash HD accounts + all legacy addresses with balances + headers
     */
    @Parcelize
    class CryptoAccountMode(
        val cryptoCurrency: CryptoCurrency,
        val hdOnly: Boolean = false,
        val isSend: Boolean = false
    ) :
        AccountMode()
}
