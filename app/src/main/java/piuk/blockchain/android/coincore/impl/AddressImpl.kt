package piuk.blockchain.android.coincore.impl

import info.blockchain.balance.CryptoCurrency
import io.reactivex.Completable
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.alg.AlgoAddress
import piuk.blockchain.android.coincore.bch.BchAddress
import piuk.blockchain.android.coincore.btc.BtcAddress
import piuk.blockchain.android.coincore.dot.PolkadotAddress
import piuk.blockchain.android.coincore.erc20.Erc20Address
import piuk.blockchain.android.coincore.eth.EthAddress
import piuk.blockchain.android.coincore.xlm.XlmAddress
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

internal fun makeExternalAssetAddress(
    asset: CryptoCurrency,
    address: String,
    label: String = address,
    environmentConfig: EnvironmentConfig,
    postTransactions: (TxResult) -> Completable = { Completable.complete() }
): CryptoAddress =
    when {
        asset.hasFeature(CryptoCurrency.IS_ERC20) -> {
            Erc20Address(
                asset = asset,
                address = address,
                label = label,
                onTxCompleted = postTransactions
            )
        }
        asset == CryptoCurrency.ETHER -> {
            EthAddress(
                address = address,
                label = label,
                onTxCompleted = postTransactions
            )
        }
        asset == CryptoCurrency.BTC -> {
            BtcAddress(
                address = address,
                label = label,
                networkParams = environmentConfig.bitcoinNetworkParameters,
                onTxCompleted = postTransactions
            )
        }
        asset == CryptoCurrency.BCH -> {
            BchAddress(
                address_ = address,
                label = label,
                onTxCompleted = postTransactions
            )
        }
        asset == CryptoCurrency.XLM -> {
            XlmAddress(
                _address = address,
                _label = label,
                onTxCompleted = postTransactions
            )
        }
        asset == CryptoCurrency.ALGO -> {
            AlgoAddress(
                address = address
            )
        }
        asset == CryptoCurrency.DOT -> {
            PolkadotAddress(
                address = address,
                label = label
            )
        }
        else -> throw IllegalArgumentException("External Address not not supported for asset: $asset")
    }
