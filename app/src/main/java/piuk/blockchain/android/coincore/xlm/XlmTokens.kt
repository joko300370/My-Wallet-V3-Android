package piuk.blockchain.android.coincore.xlm

import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.sunriver.StellarPayment
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.sunriver.fromStellarUri
import com.blockchain.sunriver.isValidXlmQr
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.service.TierService
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.impl.CryptoAssetBase
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

internal class XlmAsset(
    payloadManager: PayloadDataManager,
    private val xlmDataManager: XlmDataManager,
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
        get() = CryptoCurrency.XLM

    override fun initToken(): Completable =
        Completable.complete()

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        xlmDataManager.defaultAccount()
            .map {
                listOf(
                    XlmCryptoWalletAccount(
                        payloadManager = payloadManager,
                        account = it,
                        xlmManager = xlmDataManager,
                        exchangeRates = exchangeRates
                    )
                )
            }

    override fun parseAddress(address: String): Maybe<ReceiveAddress> =
        Maybe.fromCallable {
            if (address.isValidXlmQr()) {
                val payment = address.fromStellarUri()
                XlmAddress(
                    address = payment.public.accountId,
                    stellarPayment = payment,
                    scanUri = address
                )
            } else {
                if (isValidAddress(address)) {
                    XlmAddress(address, address)
                } else {
                    null
                }
            }
        }

    private fun isValidAddress(address: String): Boolean =
        xlmDataManager.isAddressValid(address)
}

internal data class XlmAddress(
    override val address: String,
    override val label: String = address,
    override val scanUri: String? = null,
    val stellarPayment: StellarPayment? = null
) : CryptoAddress {
    override val asset: CryptoCurrency = CryptoCurrency.XLM

    override fun equals(other: Any?): Boolean {
        return (other is XlmAddress) &&
            (other.asset == asset && other.address == address && other.label == label)
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + (scanUri?.hashCode() ?: 0)
        result = 31 * result + label.hashCode()
        result = 31 * result + (stellarPayment?.hashCode() ?: 0)
        result = 31 * result + asset.hashCode()
        return result
    }
}