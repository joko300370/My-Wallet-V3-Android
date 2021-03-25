package piuk.blockchain.android.coincore.impl

import android.content.Context
import android.content.res.Resources
import androidx.core.content.ContextCompat
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
            CryptoCurrency.YFI -> resources.getString(R.string.yfi)
            CryptoCurrency.DOT -> resources.getString(R.string.dot)
        }

    override fun colorRes(cryptoCurrency: CryptoCurrency): Int =
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> R.color.color_bitcoin_logo
            CryptoCurrency.ETHER -> R.color.color_ether_logo
            CryptoCurrency.BCH -> R.color.color_bitcoin_cash_logo
            CryptoCurrency.XLM -> R.color.color_stellar_logo
            CryptoCurrency.PAX -> R.color.color_pax_logo
            CryptoCurrency.STX -> throw NotImplementedError("STX Not implemented")
            CryptoCurrency.ALGO -> R.color.color_algo_logo
            CryptoCurrency.USDT -> R.color.color_usdt_logo
            CryptoCurrency.DGLD -> R.color.color_dgld_logo
            CryptoCurrency.AAVE -> R.color.color_aave_logo
            CryptoCurrency.YFI -> R.color.color_yfi_logo
            CryptoCurrency.DOT -> R.color.color_dot_logo
        }

    override fun chartLineColour(cryptoCurrency: CryptoCurrency, context: Context): Int =
        ContextCompat.getColor(
            context,
            when (cryptoCurrency) {
                CryptoCurrency.BTC -> R.color.color_bitcoin_logo
                CryptoCurrency.ETHER -> R.color.color_ether_logo
                CryptoCurrency.BCH -> R.color.color_bitcoin_cash_logo
                CryptoCurrency.XLM -> R.color.color_stellar_logo
                CryptoCurrency.PAX -> R.color.color_pax_logo
                CryptoCurrency.STX -> throw NotImplementedError("STX Not implemented")
                CryptoCurrency.ALGO -> R.color.color_algo_logo
                CryptoCurrency.USDT -> R.color.color_usdt_logo
                CryptoCurrency.DGLD -> R.color.dgld_chart
                CryptoCurrency.AAVE -> R.color.aave
                CryptoCurrency.YFI -> R.color.yfi
                CryptoCurrency.DOT -> R.color.dot
            }
        )

    override fun drawableResFilled(cryptoCurrency: CryptoCurrency): Int =
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> R.drawable.vector_bitcoin_colored
            CryptoCurrency.ETHER -> R.drawable.vector_eth_colored
            CryptoCurrency.BCH -> R.drawable.vector_bitcoin_cash_colored
            CryptoCurrency.XLM -> R.drawable.vector_xlm_colored
            CryptoCurrency.PAX -> R.drawable.vector_pax_colored
            CryptoCurrency.STX -> R.drawable.ic_logo_stx
            CryptoCurrency.ALGO -> R.drawable.vector_algo_colored
            CryptoCurrency.USDT -> R.drawable.vector_usdt_colored
            CryptoCurrency.DGLD -> R.drawable.vector_dgld_colored
            CryptoCurrency.AAVE -> R.drawable.vector_aave_colored
            CryptoCurrency.YFI -> R.drawable.vector_yfi_colored
            CryptoCurrency.DOT -> R.drawable.vector_dot_colored
        }

    override fun coinIconWhite(cryptoCurrency: CryptoCurrency): Int =
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> R.drawable.vector_bitcoin_white
            CryptoCurrency.ETHER -> R.drawable.vector_eth_white
            CryptoCurrency.BCH -> R.drawable.vector_bitcoin_cash_white
            CryptoCurrency.XLM -> R.drawable.vector_xlm_white
            CryptoCurrency.PAX -> R.drawable.vector_pax_white
            CryptoCurrency.ALGO -> R.drawable.vector_algo_white
            CryptoCurrency.USDT -> R.drawable.vector_usdt_white
            CryptoCurrency.DGLD -> R.drawable.vector_dgld_white
            CryptoCurrency.AAVE,
            CryptoCurrency.YFI,
            CryptoCurrency.DOT,
            CryptoCurrency.STX -> throw NotImplementedError("${cryptoCurrency.displayTicker} Not implemented")
        }

    override fun maskedAsset(cryptoCurrency: CryptoCurrency): Int =
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> R.drawable.ic_btc_circled_mask
            CryptoCurrency.XLM -> R.drawable.ic_xlm_circled_mask
            CryptoCurrency.ETHER -> R.drawable.ic_eth_circled_mask
            CryptoCurrency.PAX -> R.drawable.ic_usdd_circled_mask
            CryptoCurrency.BCH -> R.drawable.ic_bch_circled_mask
            CryptoCurrency.STX -> throw NotImplementedError("STX Not implemented")
            CryptoCurrency.ALGO -> R.drawable.ic_algo_circled_mask
            CryptoCurrency.USDT -> R.drawable.ic_usdt_circled_mask
            CryptoCurrency.DGLD -> R.drawable.ic_dgld_circled_mask
            CryptoCurrency.AAVE -> R.drawable.ic_aave_circled_mask
            CryptoCurrency.YFI -> R.drawable.ic_yfi_circled_mask
            CryptoCurrency.DOT -> R.drawable.ic_dot_circled_mask
        }

    override fun errorIcon(cryptoCurrency: CryptoCurrency): Int =
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> R.drawable.vector_btc_error
            CryptoCurrency.BCH -> R.drawable.vector_bch_error
            CryptoCurrency.ETHER -> R.drawable.vector_eth_error
            CryptoCurrency.XLM -> R.drawable.vector_xlm_error
            CryptoCurrency.PAX -> R.drawable.vector_pax_error
            CryptoCurrency.STX -> throw NotImplementedError("STX Not implemented")
            CryptoCurrency.ALGO -> R.drawable.vector_algo_error
            CryptoCurrency.USDT -> R.drawable.vector_usdt_error
            CryptoCurrency.DGLD -> R.drawable.vector_dgld_error
            CryptoCurrency.AAVE -> R.drawable.vector_aave_error
            CryptoCurrency.YFI -> R.drawable.vector_yfi_error
            CryptoCurrency.DOT -> R.drawable.vector_dot_error
        }

    override fun assetNameRes(cryptoCurrency: CryptoCurrency): Int =
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> R.string.bitcoin
            CryptoCurrency.ETHER -> R.string.ethereum
            CryptoCurrency.BCH -> R.string.bitcoin_cash
            CryptoCurrency.XLM -> R.string.lumens
            CryptoCurrency.PAX -> R.string.usd_pax_1
            CryptoCurrency.STX -> R.string.stacks_1
            CryptoCurrency.ALGO -> R.string.algorand
            CryptoCurrency.USDT -> R.string.usdt
            CryptoCurrency.DGLD -> R.string.dgld
            CryptoCurrency.AAVE -> R.string.aave
            CryptoCurrency.YFI -> R.string.yfi
            CryptoCurrency.DOT -> R.string.dot
        }

    override fun assetName(cryptoCurrency: CryptoCurrency): String =
        resources.getString(assetNameRes(cryptoCurrency))

    override fun assetTint(cryptoCurrency: CryptoCurrency): Int =
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> R.color.btc_bkgd
            CryptoCurrency.BCH -> R.color.bch_bkgd
            CryptoCurrency.ETHER -> R.color.ether_bkgd
            CryptoCurrency.PAX -> R.color.pax_bkgd
            CryptoCurrency.XLM -> R.color.xlm_bkgd
            CryptoCurrency.ALGO -> R.color.algo_bkgd
            CryptoCurrency.USDT -> R.color.usdt_bkgd
            CryptoCurrency.DGLD -> R.color.dgld_bkgd
            CryptoCurrency.AAVE -> R.color.aave_bkgd
            CryptoCurrency.YFI -> R.color.yfi_bkgd
            CryptoCurrency.DOT -> R.color.dot_bkgd
            else -> {
                android.R.color.transparent // STX left, do nothing
            }
        }

    override fun assetFilter(cryptoCurrency: CryptoCurrency): Int =
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> R.color.btc
            CryptoCurrency.BCH -> R.color.bch
            CryptoCurrency.ETHER -> R.color.eth
            CryptoCurrency.PAX -> R.color.pax
            CryptoCurrency.XLM -> R.color.xlm
            CryptoCurrency.ALGO -> R.color.algo
            CryptoCurrency.USDT -> R.color.usdt
            CryptoCurrency.DGLD -> R.color.black
            CryptoCurrency.AAVE -> R.color.aave
            CryptoCurrency.YFI -> R.color.yfi
            CryptoCurrency.DOT -> R.color.dot
            else -> {
                android.R.color.transparent // STX left, do nothing
            }
        }

    override fun makeBlockExplorerUrl(cryptoCurrency: CryptoCurrency, transactionHash: String): String =
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> "https://www.blockchain.com/btc/tx/"
            CryptoCurrency.BCH -> "https://www.blockchain.com/bch/tx/"
            CryptoCurrency.XLM -> "https://stellarchain.io/tx/"
            CryptoCurrency.ETHER,
            CryptoCurrency.PAX,
            CryptoCurrency.USDT,
            CryptoCurrency.DGLD,
            CryptoCurrency.AAVE,
            CryptoCurrency.YFI -> "https://www.blockchain.com/eth/tx/"
            CryptoCurrency.ALGO -> "https://algoexplorer.io/tx/"
            CryptoCurrency.DOT -> "https://polkascan.io/polkadot/tx/"
            CryptoCurrency.STX -> throw NotImplementedError("STX Not implemented")
        } + transactionHash

    override fun numOfDecimalsForChart(cryptoCurrency: CryptoCurrency): Int =
        when (cryptoCurrency) {
            CryptoCurrency.BTC,
            CryptoCurrency.ETHER,
            CryptoCurrency.BCH,
            CryptoCurrency.PAX,
            CryptoCurrency.ALGO,
            CryptoCurrency.USDT,
            CryptoCurrency.DGLD,
            CryptoCurrency.AAVE,
            CryptoCurrency.YFI,
            CryptoCurrency.DOT -> 2
            CryptoCurrency.XLM -> 4
            CryptoCurrency.STX -> throw NotImplementedError("STX Not implemented")
        }

    override fun fiatCurrencyIcon(currency: String): Int =
        when (currency) {
            "EUR" -> R.drawable.ic_funds_euro
            "GBP" -> R.drawable.ic_funds_gbp
            else -> R.drawable.ic_funds_usd
        }
}
