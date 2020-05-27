package com.blockchain.wallet

import info.blockchain.balance.CryptoCurrency

interface DefaultLabels {

    fun getDefaultNonCustodialWalletLabel(cryptoCurrency: CryptoCurrency): String
    fun getDefaultCustodialWalletLabel(cryptoCurrency: CryptoCurrency): String
    fun getAssetMasterWalletLabel(cryptoCurrency: CryptoCurrency): String
    fun getAllWalletLabel(): String
    fun getDefaultInterestWalletLabel(cryptoCurrency: CryptoCurrency): String
}
