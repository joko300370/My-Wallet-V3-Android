package com.blockchain.morph.exchange

import com.blockchain.logging.SwapDiagnostics
import com.blockchain.morph.exchange.mvi.Quote
import info.blockchain.balance.CryptoValue
import java.lang.StringBuilder

class SwapFailureDiagnostics : SwapDiagnostics {
    var quote: Quote? = null
    var maxSpendable: CryptoValue? = null
    var fee: CryptoValue? = null
    override var accountBalance: CryptoValue? = null

    private fun encode(): String {
        val sb = StringBuilder()

        sb.append("fix=f${quote?.fix?.ordinal ?: "X"}:")
        sb.append("type=${quote?.from?.cryptoValue?.currencyCode ?: "X"}")
        sb.append("->${quote?.to?.cryptoValue?.currencyCode ?: "X"}:")
        sb.append("in_val=${quote?.from?.cryptoValue?.amount ?: "X"}:")
        sb.append("max_val=${maxSpendable ?: "X"}:")
        sb.append("fee=${fee ?: "X"}:")
        sb.append("acct_balance=${accountBalance ?: "X"}")

        return sb.toString()
    }

    fun toLoggable(): DiagnosticLoggable = DiagnosticLoggable(encode())
}

class DiagnosticLoggable(encodedMsg: String) : Throwable(encodedMsg)
