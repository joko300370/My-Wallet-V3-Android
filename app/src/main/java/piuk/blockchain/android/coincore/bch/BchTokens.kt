package piuk.blockchain.android.coincore.bch

import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.EligibilityProvider
import com.blockchain.swap.nabu.service.TierService
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.impl.CryptoAssetBase
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateService
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager
import timber.log.Timber

private const val BCH_URL_PREFIX = "bitcoincash:"

internal class BchAsset(
    payloadManager: PayloadDataManager,
    private val bchDataManager: BchDataManager,
    private val stringUtils: StringUtils,
    custodialManager: CustodialWalletManager,
    private val environmentSettings: EnvironmentConfig,
    private val feeDataManager: FeeDataManager,
    private val sendDataManager: SendDataManager,
    exchangeRates: ExchangeRateDataManager,
    historicRates: ExchangeRateService,
    currencyPrefs: CurrencyPrefs,
    labels: DefaultLabels,
    pitLinking: PitLinking,
    crashLogger: CrashLogger,
    tiersService: TierService,
    private val walletPreferences: WalletStatus,
    eligibilityProvider: EligibilityProvider
) : CryptoAssetBase(
    payloadManager,
    exchangeRates,
    historicRates,
    currencyPrefs,
    labels,
    custodialManager,
    pitLinking,
    crashLogger,
    tiersService,
    environmentSettings,
    eligibilityProvider
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
                        BchCryptoWalletAccount.createBchAccount(
                            payloadManager = payloadManager,
                            jsonAccount = a,
                            bchManager = bchDataManager,
                            addressIndex = i,
                            isDefault = i == getDefaultAccountPosition(),
                            exchangeRates = exchangeRates,
                            networkParams = environmentSettings.bitcoinCashNetworkParameters,
                            feeDataManager = feeDataManager,
                            sendDataManager = sendDataManager,
                            walletPreferences = walletPreferences,
                            custodialWalletManager = custodialManager,
                            isArchived = a.isArchived
                        )
                    }
            }
        }

    override fun parseAddress(address: String): Maybe<ReceiveAddress> =
        Maybe.fromCallable {
            val normalisedAddress = address.removePrefix(BCH_URL_PREFIX)
            if (isValidAddress(normalisedAddress)) {
                BchAddress(normalisedAddress, address)
            } else {
                null
            }
        }

    private fun isValidAddress(address: String): Boolean =
        FormatsUtil.isValidBCHAddress(
            environmentSettings.bitcoinCashNetworkParameters,
            address
        )
}

internal class BchAddress(
    address_: String,
    override val label: String = address_,
    override val onTxCompleted: (TxResult) -> Completable = { Completable.complete() }
) : CryptoAddress {
    override val address: String = address_.removeBchUri()
    override val asset: CryptoCurrency = CryptoCurrency.BCH

    override fun toUrl(amount: CryptoValue): String {
        return "$BCH_URL_PREFIX$address"
    }
}

private fun String.removeBchUri(): String = this.replace(BCH_URL_PREFIX, "")
