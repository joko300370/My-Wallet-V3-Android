package piuk.blockchain.android.coincore

import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import piuk.blockchain.android.coincore.impl.AllWalletsAccount
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import timber.log.Timber

class Coincore internal constructor(
    // TODO: Build an interface on PayloadDataManager/PayloadManager for 'global' crypto calls; second password etc?
    private val payloadManager: PayloadDataManager,
    private val assetMap: Map<CryptoCurrency, CryptoAsset>,
    private val defaultLabels: DefaultLabels,
    private val fiatAsset: Asset
) {
    operator fun get(ccy: CryptoCurrency): CryptoAsset =
        assetMap[ccy] ?: throw IllegalArgumentException(
            "Unknown CryptoCurrency ${ccy.networkTicker}")

    fun init(): Completable =
        Completable.concat(
            assetMap.values.map { asset -> Completable.defer { asset.init() } }.toList()
        ).doOnError {
            Timber.e("Coincore initialisation failed! $it")
        }

    val fiatAssets: Asset = fiatAsset
    val cryptoAssets: Iterable<Asset> = assetMap.values.filter { it.isEnabled }
    val allAssets: Iterable<Asset> = listOf(fiatAsset) + cryptoAssets

    fun validateSecondPassword(secondPassword: String) =
        payloadManager.validateSecondPassword(secondPassword)

    fun allWallets(): Single<AccountGroup> =
        Maybe.concat(
            allAssets.map {
                it.accountGroup().map { grp -> grp.accounts }
            }
        ).reduce { a, l -> a + l }
        .map { list ->
            AllWalletsAccount(list, defaultLabels) as AccountGroup
        }.toSingle()

    fun canTransferTo(sourceAccount: CryptoAccount): Single<SingleAccountList> =
    // We only support transfers between similar assets and (soon; to - but not from - fiat)
        // at this time. If and when, say, swap is supported this will need revisiting
        Singles.zip(
            get(sourceAccount.asset).transferList(sourceAccount),
            fiatAsset.transferList(sourceAccount)
        ) { crypto, fiat ->
            crypto + fiat
        }

    fun findAccountByAddress(
        cryptoCurrency: CryptoCurrency,
        address: String
    ): Maybe<SingleAccount> =
        filterAccountsByAddress(assetMap.getValue(cryptoCurrency).accountGroup(AssetFilter.All),
            address)

    private fun filterAccountsByAddress(
        accountGroup: Maybe<AccountGroup>,
        address: String
    ): Maybe<SingleAccount> =
        accountGroup.map {
            it.accounts
        }
            .flattenAsObservable { it }
            .flatMapSingle { a ->
                a.receiveAddress
                    .map { it as CryptoAddress }
                    .onErrorReturn { NullCryptoAddress }
                    .map { ca ->
                        if (ca.address.equals(address, true)) {
                            a
                        } else {
                            NullCryptoAccount()
                        }
                    }
            }
            .filter { it != NullCryptoAccount() }
            .toList()
            .flatMapMaybe {
                if (it.isEmpty())
                    Maybe.empty<SingleAccount>()
                else
                    Maybe.just(it.first())
            }
}
