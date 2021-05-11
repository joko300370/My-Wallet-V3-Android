package com.blockchain.wallet

import info.blockchain.balance.CryptoCurrency

interface DefaultLabels {

    fun getAllWalletLabel(): String
    fun getAssetMasterWalletLabel(cryptoCurrency: CryptoCurrency): String
    fun getDefaultNonCustodialWalletLabel(cryptoCurrency: CryptoCurrency): String
    fun getOldDefaultNonCustodialWalletLabel(cryptoCurrency: CryptoCurrency): String
    fun getDefaultCustodialWalletLabel(cryptoCurrency: CryptoCurrency): String
    fun getDefaultInterestWalletLabel(cryptoCurrency: CryptoCurrency): String
    fun getDefaultExchangeWalletLabel(): String
    fun getDefaultCustodialFiatWalletLabel(fiatCurrency: String): String
}
