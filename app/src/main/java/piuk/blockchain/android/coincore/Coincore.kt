package piuk.blockchain.android.coincore

import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Completable
import piuk.blockchain.android.coincore.impl.AllWalletsAccount
import io.reactivex.Single
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import timber.log.Timber
import java.lang.IllegalArgumentException

class Coincore internal constructor(
    // TODO: Build an interface on PayloadDataManager/PayloadManager for 'global' crypto calls; second password etc?
    private val payloadManager: PayloadDataManager,
    private val assetMap: Map<CryptoCurrency, CryptoAsset>,
    private val defaultLabels: DefaultLabels,
    fiatAsset: Asset
) {
    operator fun get(ccy: CryptoCurrency): CryptoAsset =
        assetMap[ccy] ?: throw IllegalArgumentException("Unknown CryptoCurrency ${ccy.networkTicker}")

    fun init(): Completable =
        Completable.concat(
            assetMap.values.map { asset -> Completable.defer { asset.init() } }.toList()
        ).doOnError {
            Timber.e("Coincore initialisation failed! $it")
        }

    fun requireSecondPassword(): Single<Boolean> =
        Single.fromCallable { payloadManager.isDoubleEncrypted }

    val fiatAssets: Asset = fiatAsset
    val cryptoAssets: Iterable<Asset> = assetMap.values.filter { it.isEnabled }
    val allAssets: Iterable<Asset> = listOf(fiatAsset) + cryptoAssets

    fun validateSecondPassword(secondPassword: String) =
        payloadManager.validateSecondPassword(secondPassword)

    fun allWallets(): Single<AccountGroup> =
        Single.zip(
            allAssets.map { it.accountGroup() }
        ) { t: Array<Any> ->
            t.map { it as AccountGroup }
                .map { it.accounts }
                .reduce { l, g -> l + g }
        }.map {
            AllWalletsAccount(it, defaultLabels)
        }
}
