package com.blockchain.preferences

import info.blockchain.balance.CryptoCurrency

interface CurrencyPrefs {
    /**
 * @return the user's preferred Fiat currency unit
 */var fiatUnits: String
get() {
        return prefs.selectedFiatCurrency
    }
    var selectedCryptoCurrency: CryptoCurrency
    val defaultFiatCurrency: String
}
