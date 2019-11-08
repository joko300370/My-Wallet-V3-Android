package com.blockchain.balance

import android.support.annotation.ColorRes
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.androidcoreui.R

@ColorRes
fun CryptoCurrency.colorRes(): Int =
    when (this) {
        CryptoCurrency.BTC -> R.color.color_bitcoin_logo
        CryptoCurrency.ETHER -> R.color.color_ether_logo
        CryptoCurrency.BCH -> R.color.color_bitcoin_cash_logo
        CryptoCurrency.XLM -> R.color.color_stellar_logo
        CryptoCurrency.PAX -> R.color.color_pax_logo
        CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
    }
