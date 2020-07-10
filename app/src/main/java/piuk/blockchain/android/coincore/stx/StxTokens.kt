package piuk.blockchain.android.coincore.stx

import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.payload.PayloadManager
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.impl.CryptoAssetBase
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import timber.log.Timber

internal class StxAsset(
    private val payloadManager: PayloadManager,
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
        get() = CryptoCurrency.STX

    override fun initToken(): Completable =
        Completable.complete()

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.fromCallable {
            listOf(getStxAccount() as SingleAccount)
        }
        .doOnError { Timber.e(it) }
        .onErrorReturn { emptyList() }

    private fun getStxAccount(): CryptoAccount {
        val hdWallets = payloadManager.payload?.hdWallets
            ?: throw IllegalStateException("Wallet not available")

        val stxAccount = hdWallets[0].stxAccount
            ?: throw IllegalStateException("Wallet not available")

        return StxCryptoWalletAccount(
            label = "STX Account",
            address = stxAccount.bitcoinSerializedBase58Address,
            exchangeRates = exchangeRates
        )
    }

    override fun parseAddress(address: String): CryptoAddress? =
        if (isValidAddress(address)) {
            StxAddress(address)
        } else {
            null
        }

    private fun isValidAddress(address: String): Boolean =
        false
}

internal class StxAddress(
    override val address: String,
    override val label: String = address
) : CryptoAddress {
    override val asset: CryptoCurrency = CryptoCurrency.STX
}
