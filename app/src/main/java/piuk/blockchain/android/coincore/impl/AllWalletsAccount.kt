package piuk.blockchain.android.coincore.impl

import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.ExchangeRates
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.Single
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.coincore.SingleAccountList
import timber.log.Timber

class AllWalletsAccount(
    private val coincore: Coincore,
    labels: DefaultLabels
) : AccountGroup {

    override val label: String = labels.getAllWalletLabel()

    override val balance: Single<Money>
        get() = Single.error(NotImplementedError("No unified balance for All Wallets meta account"))

    override val activity: Single<ActivitySummaryList>
        get() = allActivities()

    override val actions: AvailableActions
        get() = setOf(AssetAction.ViewActivity)

    override val isFunded: Boolean
        get() = true

    override val hasTransactions: Boolean
        get() = true

    override fun fiatBalance(fiatCurrency: String, exchangeRates: ExchangeRates): Single<Money> =
        allAccounts().flattenAsObservable { it }
            .flatMapSingle { it.fiatBalance(fiatCurrency, exchangeRates) }
            .reduce { a, v -> a + v }
            .toSingle(FiatValue.zero(fiatCurrency))

    override fun includes(account: BlockchainAccount): Boolean = true

    private fun allTokens() = coincore.assets

    private fun allAccounts(): Single<List<BlockchainAccount>> =
        Single.zip(
            allTokens().map { it.accountGroup() }
        ) { t: Array<Any> ->
            t.map {
                it as BlockchainAccount
            }
        }

    private fun allActivities(): Single<ActivitySummaryList> =
        allAccounts().flattenAsObservable { it }
            .flatMapSingle { it.activity.onErrorReturn { emptyList() } }
            .reduce { a, l -> a + l }
            .doOnError { e -> Timber.e(e) }
            .toSingle(emptyList())
            .map { it.sorted() }

    override val accounts: SingleAccountList
        get() = mutableListOf<SingleAccount>().apply {
            allTokens().forEach {
                addAll(it.accounts())
            }
        }
}
