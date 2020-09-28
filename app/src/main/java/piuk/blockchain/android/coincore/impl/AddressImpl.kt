package piuk.blockchain.android.coincore.impl

import com.blockchain.extensions.exhaustive
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.bch.BchAddress
import piuk.blockchain.android.coincore.btc.BtcAddress
import piuk.blockchain.android.coincore.erc20.Erc20Address
import piuk.blockchain.android.coincore.eth.EthAddress
import piuk.blockchain.android.coincore.xlm.XlmAddress
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import java.lang.IllegalArgumentException

internal fun makeExternalAssetAddress(
    asset: CryptoCurrency,
    address: String,
    label: String,
    environmentConfig: EnvironmentConfig
): CryptoAddress =
    when (asset) {
        CryptoCurrency.PAX,
        CryptoCurrency.USDT -> {
            Erc20Address(
                asset = asset,
                address = address,
                label = label
            )
        }
        CryptoCurrency.ETHER -> {
            EthAddress(
                address = address,
                label = label
            )
        }
        CryptoCurrency.BTC -> {
            BtcAddress(
                address = address,
                label = label,
                networkParams = environmentConfig.bitcoinNetworkParameters
            )
        }
        CryptoCurrency.BCH -> {
            BchAddress(
                address_ = address,
                label = label
            )
        }
        CryptoCurrency.XLM -> {
            XlmAddress(
                address = address,
                label = label
            )
        }
        CryptoCurrency.ALGO,
        CryptoCurrency.STX -> throw IllegalArgumentException("External Address not not supported for asset: $asset")
    }.exhaustive
