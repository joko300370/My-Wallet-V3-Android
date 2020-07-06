package piuk.blockchain.android.coincore.impl

import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.SendState
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import timber.log.Timber

class AllWalletsAccount(
    private val coincore: Coincore,
    labels: DefaultLabels
) : CryptoAccount {

    override val label: String = labels.getAllWalletLabel()

    override val cryptoCurrencies: Set<CryptoCurrency>
        get() = CryptoCurrency.activeCurrencies().toSet()

    override val balance: Single<CryptoValue>
        get() = Single.error(NotImplementedError("No crypto balance for All Wallets meta account"))

    override val activity: Single<ActivitySummaryList>
        get() = allActivities()

    override val actions: AvailableActions
        get() = setOf(AssetAction.ViewActivity)

    override val isFunded: Boolean
        get() = true

    override val hasTransactions: Boolean
        get() = true

    override fun fiatBalance(fiat: String, exchangeRates: ExchangeRateDataManager): Single<FiatValue> =
        allAccounts().flattenAsObservable { it }
            .flatMapSingle { it.fiatBalance(fiat, exchangeRates) }
            .reduce { a, v -> a + v }
            .toSingle(FiatValue.zero(fiat))

    override fun includes(cryptoAccount: CryptoSingleAccount): Boolean = true

    override val sendState: Single<SendState>
        get() = Single.just(SendState.NOT_SUPPORTED)

    private fun allTokens() = CryptoCurrency.activeCurrencies().map { coincore[it] }

    private fun allAccounts(): Single<List<CryptoAccount>> =
        Single.zip(
            allTokens().map { it.accounts() }
        ) { t: Array<Any> ->
            t.map { it as CryptoAccount }
        }

    fun allActivities(): Single<ActivitySummaryList> =
        allAccounts().flattenAsObservable { it }
            .flatMapSingle { it.activity.onErrorReturn { emptyList() } }
            .reduce { a, l -> a + l }
            .doOnError { e -> Timber.e(e) }
            .toSingle(emptyList())
            .map { it.sorted() }
}
