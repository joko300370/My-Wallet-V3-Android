package piuk.blockchain.android.coincore

import info.blockchain.balance.CryptoCurrency

interface AssetResources {
    fun getDisplayName(cryptoCurrency: CryptoCurrency): String
}