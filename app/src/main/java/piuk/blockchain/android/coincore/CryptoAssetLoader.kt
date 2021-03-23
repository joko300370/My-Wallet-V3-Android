package piuk.blockchain.android.coincore

import info.blockchain.balance.CryptoCurrency

class CryptoAssetLoader(private val cryptoAssets: List<CryptoAsset>) : AssetLoader {
    override val assetMap: Map<CryptoCurrency, CryptoAsset>
        get() = cryptoAssets.withEthBeforeAnyErc20().map { it.asset to it }.toMap()

    // this ensures us that eth.init() happens before any other Erc20 token we may have
    private fun List<CryptoAsset>.withEthBeforeAnyErc20(): List<CryptoAsset> {
        val ethItem = this.firstOrNull { it.asset == CryptoCurrency.ETHER } ?: return this
        val indexOfFirstErc20 = this.indexOfFirst { it.asset.hasFeature(CryptoCurrency.IS_ERC20) }
        indexOfFirstErc20.takeIf { it != -1 }?.let {
            return this.toMutableList().apply {
                remove(ethItem)
                add(indexOfFirstErc20, ethItem)
                toList()
            }
        } ?: return this
    }
}

// TODO this will change to support both fiat and crypto, when we have a common interface/class for both
interface AssetLoader {
    val assetMap: Map<CryptoCurrency, CryptoAsset>
}