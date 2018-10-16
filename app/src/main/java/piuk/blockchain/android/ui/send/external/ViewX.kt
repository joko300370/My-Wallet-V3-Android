package piuk.blockchain.android.ui.send.external

import info.blockchain.balance.CryptoCurrency

interface ViewX : piuk.blockchain.androidcoreui.ui.base.View {

    fun setSelectedCurrency(cryptoCurrency: CryptoCurrency)
}
