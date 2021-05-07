package piuk.blockchain.android.coincore.impl

import info.blockchain.api.BitcoinApi
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.payload.BalanceCall
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.lang.IllegalStateException
import java.math.BigInteger

class OfflineBalanceCall(
    private val bitcoinApi: BitcoinApi
) {
    fun getBalanceOfAddresses(
        asset: CryptoCurrency,
        address: List<String>
    ): Single<Map<String, CryptoValue>> =
        Single.fromCallable {
            when (asset) {
                CryptoCurrency.BTC -> getBalanceForBTC(address)
                CryptoCurrency.BCH -> getBalanceForBCH(address)
                else -> throw IllegalStateException("Offline call not valid for ${asset.networkTicker}")
            }
        }.subscribeOn(Schedulers.io())
            .map { it.mapValues { v -> CryptoValue.fromMinor(asset, v.value) } }
            .doOnError { Timber.e("Error making balance call") }

    private fun getBalanceForBTC(addresses: List<String>): Map<String, BigInteger> =
        BalanceCall(bitcoinApi, CryptoCurrency.BTC).getBalancesForAddresses(addresses)

    private fun getBalanceForBCH(addresses: List<String>): Map<String, BigInteger> =
        BalanceCall(bitcoinApi, CryptoCurrency.BCH).getBalancesForAddresses(addresses)
}
