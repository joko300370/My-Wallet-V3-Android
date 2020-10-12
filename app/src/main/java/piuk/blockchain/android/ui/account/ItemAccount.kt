package piuk.blockchain.android.ui.account

import com.blockchain.serialization.JsonSerializableAccount
import info.blockchain.balance.CryptoValue

data class ItemAccount @JvmOverloads constructor(
    val label: String = "",
    val balance: CryptoValue? = null,
    val tag: String = "",
    // Ultimately this is used to sign txs
    var accountObject: JsonSerializableAccount? = null,
    // Address/Xpub to fetch balance/tx list
    val address: String = "",
    val type: TYPE = TYPE.SINGLE_ACCOUNT
) {
    enum class TYPE {
        ALL_ACCOUNTS_AND_LEGACY, ALL_LEGACY, SINGLE_ACCOUNT
    }
}

fun ItemAccount.formatDisplayBalance() =
        balance?.toStringWithSymbol() ?: ""
