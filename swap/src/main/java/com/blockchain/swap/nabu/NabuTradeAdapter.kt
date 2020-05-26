package com.blockchain.swap.nabu

import com.blockchain.morph.CoinPair
import com.blockchain.swap.common.trade.MorphTrade
import com.blockchain.swap.common.trade.MorphTradeOrder
import com.blockchain.swap.nabu.api.nabu.NabuTransaction
import com.blockchain.swap.nabu.api.trade.TransactionState
import com.blockchain.swap.nabu.extensions.fromIso8601ToUtc
import com.blockchain.swap.nabu.extensions.toLocalTime
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import java.math.BigDecimal

internal class NabuTradeAdapter(private val trade: NabuTransaction) :
    MorphTrade {

    override val timestamp: Long
        get() = trade.createdAt.fromIso8601ToUtc()?.toLocalTime()?.time?.div(1000)
            ?: System.currentTimeMillis() / 1000

    override val hashOut: String?
        get() = trade.hashOut

    override val status: MorphTrade.Status
        get() = trade.state.map()

    override val quote: MorphTradeOrder
        get() = object : MorphTradeOrder {
            override val pair: CoinPair
                get() = trade.pair

            override val orderId: String
                get() = trade.id

            override val depositAmount: CryptoValue
                get() = trade.deposit

            override val withdrawalAmount: CryptoValue
                get() = trade.withdrawal

            override val quotedRate: BigDecimal
                get() = BigDecimal.ZERO

            override val minerFee: CryptoValue
                get() = trade.fee

            override val fiatValue: FiatValue
                get() = trade.fiatValue
        }

    override val withdrawalAddress: String
        get() = trade.withdrawalAddress

    override val depositAddress: String
        get() = trade.depositAddress

    override fun enoughInfoForDisplay() = true
}

internal fun TransactionState.map(): MorphTrade.Status =
    when (this) {
        TransactionState.Delayed,
        TransactionState.PendingWithdrawal,
        TransactionState.FinishedDeposit,
        TransactionState.PendingDeposit,
        TransactionState.PendingExecution -> MorphTrade.Status.IN_PROGRESS
        TransactionState.Finished -> MorphTrade.Status.COMPLETE
        TransactionState.PendingRefund -> MorphTrade.Status.REFUND_IN_PROGRESS
        TransactionState.Failed -> MorphTrade.Status.FAILED
        TransactionState.Expired -> MorphTrade.Status.EXPIRED
        TransactionState.Refunded -> MorphTrade.Status.REFUNDED
    }
