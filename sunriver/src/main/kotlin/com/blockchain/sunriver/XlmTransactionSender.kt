package com.blockchain.sunriver

import info.blockchain.balance.CryptoValue
import io.reactivex.Completable

interface XlmTransactionSender {

    fun sendFunds(
        value: CryptoValue,
        toAccountId: String
    ): Completable
}
