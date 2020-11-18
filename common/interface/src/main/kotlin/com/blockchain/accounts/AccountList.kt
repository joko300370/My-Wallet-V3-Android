package com.blockchain.accounts

import info.blockchain.balance.AccountReference
import info.blockchain.balance.AccountReferenceList
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single

interface AccountList {
    @Deprecated("Use defaultAccount() instead")
    fun defaultAccountReference(): AccountReference
    fun defaultAccount(): Single<AccountReference>
    fun accounts(): Single<AccountReferenceList>
}

@Deprecated("Only used in old swap")
interface AsyncAllAccountList {
    fun allAccounts(): Single<AccountReferenceList>
    operator fun get(cryptoCurrency: CryptoCurrency): Single<AccountList>
}
