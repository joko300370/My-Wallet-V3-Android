package piuk.blockchain.android.coincore.stx

import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.impl.CryptoAssetBase
import piuk.blockchain.android.coincore.impl.OfflineAccountUpdater
import piuk.blockchain.android.identity.UserIdentity
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateService
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import timber.log.Timber

internal class StxAsset(
    payloadManager: PayloadDataManager,
    custodialManager: CustodialWalletManager,
    exchangeRates: ExchangeRateDataManager,
    historicRates: ExchangeRateService,
    currencyPrefs: CurrencyPrefs,
    labels: DefaultLabels,
    pitLinking: PitLinking,
    crashLogger: CrashLogger,
    identity: UserIdentity,
    features: InternalFeatureFlagApi,
    offlineAccounts: OfflineAccountUpdater
) : CryptoAssetBase(
    payloadManager,
    exchangeRates,
    historicRates,
    currencyPrefs,
    labels,
    custodialManager,
    pitLinking,
    crashLogger,
    offlineAccounts,
    identity,
    features
) {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.STX

    override fun initToken(): Completable =
        Completable.complete()

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.fromCallable {
            listOf(getStxAccount() as SingleAccount)
        }
            .doOnError { Timber.e(it) }
            .onErrorReturn { emptyList() }

    private fun getStxAccount(): CryptoAccount {
        val stxAccount = payloadManager.stxAccount

        return StxCryptoWalletAccount(
            payloadManager,
            label = "STX Account",
            address = stxAccount.bitcoinSerializedBase58Address,
            exchangeRates = exchangeRates,
            custodialWalletManager = custodialManager,
            identity = identity
        )
    }

    override fun parseAddress(address: String, label: String?): Maybe<ReceiveAddress> =
        Maybe.fromCallable {
            if (isValidAddress(address)) {
                StxAddress(address, label ?: address)
            } else {
                null
            }
        }

    override fun isValidAddress(address: String): Boolean =
        false
}

internal class StxAddress(
    override val address: String,
    override val label: String = address
) : CryptoAddress {
    override val asset: CryptoCurrency = CryptoCurrency.STX
}
