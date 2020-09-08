package piuk.blockchain.android.coincore.btc

import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.service.TierService
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.uri.BitcoinURI
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.impl.CryptoAssetBase
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

private const val BTC_URL_PREFIX = "bitcoin:"

internal class BtcAsset(
    payloadManager: PayloadDataManager,
    private val networkParameters: NetworkParameters,
    custodialManager: CustodialWalletManager,
    exchangeRates: ExchangeRateDataManager,
    historicRates: ChartsDataManager,
    currencyPrefs: CurrencyPrefs,
    labels: DefaultLabels,
    pitLinking: PitLinking,
    crashLogger: CrashLogger,
    tiersService: TierService
) : CryptoAssetBase(
    payloadManager,
    exchangeRates,
    historicRates,
    currencyPrefs,
    labels,
    custodialManager,
    pitLinking,
    crashLogger,
    tiersService
) {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.BTC

    override fun initToken(): Completable =
        Completable.complete()

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.fromCallable {
            with(payloadManager) {
                val result = mutableListOf<CryptoAccount>()
                val defaultIndex = defaultAccountIndex
                accounts.forEachIndexed { i, a ->
                    result.add(
                        BtcCryptoWalletAccount(
                            a,
                            payloadManager,
                            i == defaultIndex,
                            exchangeRates,
                            networkParameters
                        )
                    )
                }

                legacyAddresses.forEach { a ->
                    result.add(
                        BtcCryptoWalletAccount(
                            a,
                            payloadManager,
                            exchangeRates,
                            networkParameters
                        )
                    )
                }
                result
            }
        }

    override fun parseAddress(address: String): Maybe<ReceiveAddress> =
        Maybe.fromCallable {
            if (isValidAddress(address)) {
                BtcAddress(
                    address = address,
                    networkParams = networkParameters,
                    scanUri = address
                )
            } else {
                null
            }
        }

        private fun isValidAddress(address: String): Boolean =
            FormatsUtil.isValidBitcoinAddress(
                networkParameters,
                address
            )
}

internal class BtcAddress(
    override val address: String,
    override val scanUri: String? = null,
    override val label: String = address,
    private val networkParams: NetworkParameters
) : CryptoAddress {
    override val asset: CryptoCurrency = CryptoCurrency.BTC

    override fun toUrl(amount: CryptoValue): String {
        return if (amount.isPositive) {
            BitcoinURI.convertToBitcoinURI(
                Address.fromBase58(networkParams, address),
                Coin.valueOf(amount.toBigInteger().toLong()),
                "",
                ""
            )
        } else {
            return "$BTC_URL_PREFIX$address"
            }
        }
}
