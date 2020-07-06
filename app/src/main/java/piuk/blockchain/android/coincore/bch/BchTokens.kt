package piuk.blockchain.android.coincore.bch

import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.CryptoSingleAccountList
import piuk.blockchain.android.coincore.impl.AssetTokensBase
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import timber.log.Timber

internal class BchTokens(
    private val bchDataManager: BchDataManager,
    private val stringUtils: StringUtils,
    custodialManager: CustodialWalletManager,
    private val environmentSettings: EnvironmentConfig,
    exchangeRates: ExchangeRateDataManager,
    historicRates: ChartsDataManager,
    currencyPrefs: CurrencyPrefs,
    labels: DefaultLabels,
    pitLinking: PitLinking,
    crashLogger: CrashLogger
) : AssetTokensBase(
    exchangeRates,
    historicRates,
    currencyPrefs,
    labels,
    custodialManager,
    pitLinking,
    crashLogger
) {
    override val asset: CryptoCurrency
        get() = CryptoCurrency.BCH

    override fun initToken(): Completable =
        bchDataManager.initBchWallet(stringUtils.getString(R.string.bch_default_account_label))
            .doOnError { Timber.e("Unable to init BCH, because: $it") }
            .onErrorComplete()

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<CryptoSingleAccountList> =
        Single.fromCallable {
            with(bchDataManager) {
                val result = mutableListOf<CryptoSingleAccount>()
                val defaultIndex = getDefaultAccountPosition()

                val accounts = getAccountMetadataList()
                accounts.forEachIndexed { i, a ->
                    result.add(
                        BchCryptoWalletAccount(
                            a,
                            bchDataManager,
                            i == defaultIndex,
                            exchangeRates,
                            environmentSettings.bitcoinCashNetworkParameters
                        )
                    )
                }
                result
            }
        }

    override fun parseAddress(address: String): CryptoAddress? =
        null

    private fun isValidAddress(address: String): Boolean =
        FormatsUtil.isValidBitcoinCashAddress(
            environmentSettings.bitcoinCashNetworkParameters,
            address
        )
}

internal class BchAddress(
    override val address: String,
    override val label: String = address
) : CryptoAddress {
    override val asset: CryptoCurrency = CryptoCurrency.BCH
}
