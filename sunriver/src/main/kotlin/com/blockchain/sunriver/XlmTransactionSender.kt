package com.blockchain.sunriver

import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoValue
import io.reactivex.Completable

interface XlmTransactionSender {

    fun sendFunds(
        from: AccountReference,
        value: CryptoValue,
        toAccountId: String
    ): Completable
}
