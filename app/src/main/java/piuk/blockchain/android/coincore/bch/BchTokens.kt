package piuk.blockchain.android.coincore.bch

import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.service.TierService
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.impl.CryptoAssetBase
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import timber.log.Timber

internal class BchAsset(
    payloadManager: PayloadDataManager,
    private val bchDataManager: BchDataManager,
    private val stringUtils: StringUtils,
    custodialManager: CustodialWalletManager,
    private val environmentSettings: EnvironmentConfig,
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
        get() = CryptoCurrency.BCH

    override fun initToken(): Completable =
        bchDataManager.initBchWallet(stringUtils.getString(R.string.bch_default_account_label))
            .doOnError { Timber.e("Unable to init BCH, because: $it") }
            .onErrorComplete()

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.fromCallable {
            with(bchDataManager) {
                getAccountMetadataList()
                    .mapIndexed { i, a ->
                        BchCryptoWalletAccount(
                            payloadManager = payloadManager,
                            jsonAccount = a,
                            bchManager = bchDataManager,
                            isDefault = i == getDefaultAccountPosition(),
                            exchangeRates = exchangeRates,
                            networkParams = environmentSettings.bitcoinCashNetworkParameters
                        )
                }
            }
        }

    override fun parseAddress(address: String): Maybe<ReceiveAddress> =
        Maybe.fromCallable {
            if (isValidAddress(address)) {
                BchAddress(address)
            } else {
                null
            }
        }

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
