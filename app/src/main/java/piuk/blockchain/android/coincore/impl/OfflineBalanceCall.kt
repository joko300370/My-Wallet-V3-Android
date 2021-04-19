package piuk.blockchain.android.coincore.impl

import info.blockchain.api.BitcoinApi
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.payload.BalanceCall
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
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

    private fun getBalanceForBTC(addresses: List<String>): Map<String, BigInteger> {
        val legacy = addresses.filter { FormatsUtil.isValidLegacyBtcAddress(it) }
            .map { XPubs(XPub(address = it, derivation = XPub.Format.LEGACY)) }
        val segwit = addresses.filter { FormatsUtil.isValidBech32BtcAddress(it) }
            .map { XPubs(XPub(address = it, derivation = XPub.Format.SEGWIT)) }

        val xpubs = legacy + segwit
        return BalanceCall(bitcoinApi, CryptoCurrency.BTC).getBalancesFor(xpubs)
    }

    private fun getBalanceForBCH(addresses: List<String>): Map<String, BigInteger> {
        val xpubs = addresses.map { XPubs(XPub(address = it, derivation = XPub.Format.LEGACY)) }
        return BalanceCall(bitcoinApi, CryptoCurrency.BCH).getBalancesFor(xpubs)
    }
}
