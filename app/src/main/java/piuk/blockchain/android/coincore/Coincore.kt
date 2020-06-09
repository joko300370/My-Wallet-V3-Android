package piuk.blockchain.android.coincore

import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.impl.AllWalletsAccount
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import timber.log.Timber
import java.lang.IllegalArgumentException

class Coincore internal constructor(
    // TODO: Build an interface on PayloadDataManager/PayloadManager for 'global' crypto calls; second password etc?
    private val payloadManager: PayloadDataManager,
    private val tokenMap: Map<CryptoCurrency, AssetTokens>,
    private val defaultLabels: DefaultLabels
) {
    operator fun get(ccy: CryptoCurrency): AssetTokens =
        tokenMap[ccy] ?: throw IllegalArgumentException("Unknown CryptoCurrency ${ccy.networkTicker}")

    fun init(): Completable =
        Completable.concat(
            tokenMap.values.map { token -> Completable.defer { token.init() } }.toList()
        ).doOnError {
            Timber.e("Coincore initialisation failed! $it")
        }

    fun requireSecondPassword(): Single<Boolean> =
        Single.fromCallable { payloadManager.isDoubleEncrypted }

    val tokens: Iterable<AssetTokens>
        = tokenMap.values

    val allWallets: CryptoAccount by lazy {
        AllWalletsAccount(this, defaultLabels)
    }
}
