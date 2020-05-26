package com.blockchain.preferences

import info.blockchain.balance.CryptoCurrency

interface CurrencyPrefs {
    var selectedFiatCurrency: String
    var selectedCryptoCurrency: CryptoCurrency
    val defaultFiatCurrency: String
}
