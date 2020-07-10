package piuk.blockchain.android.coincore.btc

import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.impl.CryptoAssetBase
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

internal class BtcAsset(
    private val payloadDataManager: PayloadDataManager,
    private val environmentSettings: EnvironmentConfig,
    custodialManager: CustodialWalletManager,
    exchangeRates: ExchangeRateDataManager,
    historicRates: ChartsDataManager,
    currencyPrefs: CurrencyPrefs,
    labels: DefaultLabels,
    pitLinking: PitLinking,
    crashLogger: CrashLogger
) : CryptoAssetBase(
    exchangeRates,
    historicRates,
    currencyPrefs,
    labels,
    custodialManager,
    pitLinking,
    crashLogger
) {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.BTC

    override fun initToken(): Completable =
        Completable.complete()

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.fromCallable {
            with(payloadDataManager) {
                val result = mutableListOf<CryptoAccount>()
                val defaultIndex = defaultAccountIndex
                accounts.forEachIndexed { i, a ->
                    result.add(
                        BtcCryptoWalletAccount(
                            a,
                            payloadDataManager,
                            i == defaultIndex,
                            exchangeRates
                        )
                    )
                }

                legacyAddresses.forEach { a ->
                    result.add(
                        BtcCryptoWalletAccount(
                            a,
                            payloadDataManager,
                            exchangeRates
                        )
                    )
                }
                result
            }
        }

    override fun parseAddress(address: String): CryptoAddress? =
        if (isValidAddress(address)) {
            BtcAddress(address)
        } else {
            null
        }

    private fun isValidAddress(address: String): Boolean =
        FormatsUtil.isValidBitcoinAddress(
            environmentSettings.bitcoinNetworkParameters,
            address
        )
}

internal class BtcAddress(
    override val address: String,
    override val label: String = address
) : CryptoAddress {
    override val asset: CryptoCurrency = CryptoCurrency.BTC
}
