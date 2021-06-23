package piuk.blockchain.android.coincore.fiat

import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.repositories.CustodialAssetWalletsBalancesRepository
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.wallet.DefaultLabels
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.Asset
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

class FiatAsset(
    private val labels: DefaultLabels,
    private val custodialAssetWalletsBalancesRepository: CustodialAssetWalletsBalancesRepository,
    private val exchangeRateDataManager: ExchangeRateDataManager,
    private val custodialWalletManager: CustodialWalletManager,
    private val currencyPrefs: CurrencyPrefs
) : Asset {
    override fun init(): Completable = Completable.complete()
    override val isEnabled: Boolean = true

    override fun accountGroup(filter: AssetFilter): Maybe<AccountGroup> =
        when (filter) {
            AssetFilter.All,
            AssetFilter.Custodial -> fetchFiatWallets()
            AssetFilter.NonCustodial,
            AssetFilter.Interest -> Maybe.empty() // Only support single accounts
        }

    private fun setSelectedFiatFirst(fiatList: List<String>): List<String> {
        val fiatMutableList = fiatList.toMutableList()
        if (fiatMutableList.first() != currencyPrefs.selectedFiatCurrency) {
            fiatMutableList.firstOrNull { it == currencyPrefs.selectedFiatCurrency }?.let {
                fiatMutableList.remove(it)
                fiatMutableList.add(0, it)
            }
        }
        return fiatMutableList.toList()
    }

    private fun fetchFiatWallets(): Maybe<AccountGroup> =
        custodialWalletManager.getSupportedFundsFiats(
            currencyPrefs.selectedFiatCurrency
        )
            .flatMapMaybe { fiatList ->
                val mutable = fiatList.toMutableList()
                if (mutable.isNotEmpty()) {
                    val orderedList = setSelectedFiatFirst(fiatList)
                    Maybe.just(
                        FiatAccountGroup(
                            label = "Fiat Accounts",
                            accounts = orderedList.map { getAccount(it) }
                        )
                    )
                } else {
                    Maybe.empty()
                }
            }

    private val accounts = mutableMapOf<String, FiatAccount>()

    private fun getAccount(fiatCurrency: String): FiatAccount =
        accounts.getOrPut(fiatCurrency) {
            FiatCustodialAccount(
                label = labels.getDefaultCustodialFiatWalletLabel(fiatCurrency),
                fiatCurrency = fiatCurrency,
                custodialAssetWalletsBalancesRepository = custodialAssetWalletsBalancesRepository,
                exchangesRatesDataManager = exchangeRateDataManager,
                custodialWalletManager = custodialWalletManager
            )
        }

    // we cannot transfer for fiat
    override fun transactionTargets(account: SingleAccount): Single<SingleAccountList> =
        Single.just(emptyList())

    override fun parseAddress(address: String, label: String?): Maybe<ReceiveAddress> = Maybe.empty()
}
