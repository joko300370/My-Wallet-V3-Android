package piuk.blockchain.android.coincore.fiat

import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.Asset
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.SingleAccount

class FiatAsset : Asset {
    override fun init(): Completable = Completable.complete()

    private val accounts = listOf(
        FiatCustodialAccount(
            label = "USD Account",
            fiatCurrency = "USD",
            isDefault = true,
            funds = FiatValue.fromMajor("USD", 1001.99.toBigDecimal())
        ),
        FiatCustodialAccount(
            label = "EUR Account",
            fiatCurrency = "EUR",
            funds = FiatValue.fromMajor("USD", 26.25.toBigDecimal())
        ),
        FiatCustodialAccount(
            label = "GBP Account",
            fiatCurrency = "GBP",
            funds = FiatValue.fromMajor("GBP", 499.95.toBigDecimal())
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
