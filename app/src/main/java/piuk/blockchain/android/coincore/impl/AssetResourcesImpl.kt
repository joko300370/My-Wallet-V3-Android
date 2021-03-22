package piuk.blockchain.android.coincore.impl

import android.content.res.Resources
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetResources

internal class AssetResourcesImpl(val resources: Resources) : AssetResources {
    override fun getDisplayName(cryptoCurrency: CryptoCurrency) =
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> resources.getString(R.string.bitcoin)
            CryptoCurrency.ETHER -> resources.getString(R.string.ether)
            CryptoCurrency.BCH -> resources.getString(R.string.bitcoin_cash)
            CryptoCurrency.XLM -> resources.getString(R.string.lumens)
            CryptoCurrency.ALGO -> resources.getString(R.string.algorand)
            CryptoCurrency.DGLD -> resources.getString(R.string.dgld)
            CryptoCurrency.PAX -> resources.getString(R.string.usd_pax_1)
            CryptoCurrency.USDT -> resources.getString(R.string.usdt)
            CryptoCurrency.STX -> resources.getString(R.string.stacks_1)
            CryptoCurrency.AAVE -> resources.getString(R.string.aave)
        }
}
