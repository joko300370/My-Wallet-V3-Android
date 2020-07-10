package piuk.blockchain.android.coincore.fiat

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.repositories.AssetBalancesRepository
import com.blockchain.wallet.DefaultLabels
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.Asset
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

class FiatAsset(
    labels: DefaultLabels,
    assetBalancesRepository: AssetBalancesRepository,
    exchangeRateDataManager: ExchangeRateDataManager,
    custodialWalletManager: CustodialWalletManager
) :
    Asset {
    override fun init(): Completable = Completable.complete()
    override val isEnabled: Boolean = true

    private val accounts = listOf(
        FiatCustodialAccount(
            label = labels.getDefaultCustodialFiatWalletLabel("EUR"),
            fiatCurrency = "EUR",
            assetBalancesRepository = assetBalancesRepository,
            exchangesRatesDataManager = exchangeRateDataManager,
            custodialWalletManager = custodialWalletManager
        ),
        FiatCustodialAccount(
            label = labels.getDefaultCustodialFiatWalletLabel("GBP"),
            fiatCurrency = "GBP",
            assetBalancesRepository = assetBalancesRepository,
            exchangesRatesDataManager = exchangeRateDataManager,
            custodialWalletManager = custodialWalletManager
        )
    )

    override fun defaultAccount(): Single<SingleAccount> =
        Single.just(accounts.first())

    override fun accountGroup(filter: AssetFilter): Single<AccountGroup> =
        Single.just(
            FiatAccountGroup(
                label = "Fiat Accounts",
                accounts = accounts
            )
        )

    override fun accounts(): SingleAccountList = accounts

    override fun canTransferTo(account: BlockchainAccount): Single<SingleAccountList> =
        Single.just(emptyList())

    override fun parseAddress(address: String): CryptoAddress? = null
}
