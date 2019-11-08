package com.blockchain.balance

import android.support.annotation.DrawableRes
import android.support.v7.content.res.AppCompatResources
import android.widget.ImageView
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.androidcoreui.R

@DrawableRes
fun CryptoCurrency.drawableResFilled(): Int =
    when (this) {
        CryptoCurrency.BTC -> R.drawable.vector_bitcoin_colored
        CryptoCurrency.ETHER -> R.drawable.vector_eth_colored
        CryptoCurrency.BCH -> R.drawable.vector_bitcoin_cash_colored
        CryptoCurrency.XLM -> R.drawable.vector_xlm_colored
        CryptoCurrency.PAX -> R.drawable.vector_pax_colored
        CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
    }

@DrawableRes
fun CryptoCurrency.coinIconWhite(): Int =
    when (this) {
        CryptoCurrency.BTC -> R.drawable.vector_bitcoin_white
        CryptoCurrency.ETHER -> R.drawable.vector_eth_white
        CryptoCurrency.BCH -> R.drawable.vector_bitcoin_cash_white
        CryptoCurrency.XLM -> R.drawable.vector_xlm_white
        CryptoCurrency.PAX -> R.drawable.vector_pax_white
        CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
    }

fun ImageView.setImageDrawable(@DrawableRes res: Int) {
    setImageDrawable(AppCompatResources.getDrawable(context, res))
}

@DrawableRes
fun CryptoCurrency.errorIcon(): Int =
    when (this) {
        CryptoCurrency.BTC -> R.drawable.vector_btc_error
        CryptoCurrency.BCH -> R.drawable.vector_bch_error
        CryptoCurrency.ETHER -> R.drawable.vector_eth_error
        CryptoCurrency.XLM -> R.drawable.vector_xlm_error
        CryptoCurrency.PAX -> R.drawable.vector_pax_error
        CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
    }