package piuk.blockchain.android.ui.send.external

import info.blockchain.balance.CryptoCurrency

interface ViewX : piuk.blockchain.androidcoreui.ui.base.View {

    fun setSelectedCurrency(cryptoCurrency: CryptoCurrency)

    fun updateFiatCurrency(currency: String)

    fun updateReceivingHintAndAccountDropDowns(currency: CryptoCurrency, listSize: Int)

    fun updateCryptoAmountWithoutTriggeringListener(amountString: String?)

    fun updateFiatAmountWithoutTriggeringListener(amountString: String?)
}
