package piuk.blockchain.android.coincore.alg

import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.CryptoSingleAccountList
import piuk.blockchain.android.coincore.impl.AssetTokensBase
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import timber.log.Timber

internal class AlgoTokens(
    custodialManager: CustodialWalletManager,
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
        get() = CryptoCurrency.ALGO

    override fun initToken(): Completable =
        Completable.complete()

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<CryptoSingleAccountList> =
        Single.fromCallable {
            listOf(getAlgoAccount())
        }
        .doOnError { Timber.e(it) }
        .onErrorReturn { emptyList() }

    private fun getAlgoAccount(): CryptoSingleAccount =
        AlgoCryptoWalletAccount(label = labels.getDefaultNonCustodialWalletLabel(asset),
            exchangeRates = exchangeRates)

    override fun loadCustodialAccount(): Single<CryptoSingleAccountList> =
        Single.just(
            listOf(AlgoCustodialTradingAccount(
                asset,
                labels.getDefaultCustodialWalletLabel(asset),
                exchangeRates,
                custodialManager
            ))
        )

    override fun parseAddress(address: String): CryptoAddress? = null
}
