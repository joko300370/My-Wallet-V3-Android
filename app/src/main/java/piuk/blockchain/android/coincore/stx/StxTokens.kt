package piuk.blockchain.android.coincore.stx

import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.payload.PayloadManager
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.CryptoSingleAccountList
import piuk.blockchain.android.coincore.impl.AssetTokensBase
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import timber.log.Timber

internal class StxTokens(
    private val payloadManager: PayloadManager,
    custodialManager: CustodialWalletManager,
    exchangeRates: ExchangeRateDataManager,
    historicRates: ChartsDataManager,
    currencyPrefs: CurrencyPrefs,
    labels: DefaultLabels,
    crashLogger: CrashLogger,
    rxBus: RxBus
) : AssetTokensBase(
    exchangeRates,
    historicRates,
    currencyPrefs,
    labels,
    custodialManager,
    crashLogger,
    rxBus
) {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.STX

    override fun initToken(): Completable =
        Completable.complete()

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<CryptoSingleAccountList> =
        Single.fromCallable {
            listOf(getStxAccount())
        }
        .doOnError { Timber.e(it) }
        .onErrorReturn { emptyList() }

    private fun getStxAccount(): CryptoSingleAccount {
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
}
