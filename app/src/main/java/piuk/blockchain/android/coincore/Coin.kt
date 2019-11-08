package piuk.blockchain.android.coincore

import info.blockchain.balance.AccountReference
import io.reactivex.Single

interface Coin {
    fun defaultAccount(): Single<AccountReference>
}