package piuk.blockchain.android.coincore.dot

import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.SimpleBuyEligibilityProvider
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.impl.CryptoAssetBase
import piuk.blockchain.android.coincore.impl.OfflineAccountUpdater
import piuk.blockchain.android.identity.UserIdentity
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateService
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import java.util.concurrent.atomic.AtomicBoolean

internal class PolkadotAsset(
    payloadManager: PayloadDataManager,
    custodialManager: CustodialWalletManager,
    exchangeRates: ExchangeRateDataManager,
    historicRates: ExchangeRateService,
    currencyPrefs: CurrencyPrefs,
    labels: DefaultLabels,
    pitLinking: PitLinking,
    crashLogger: CrashLogger,
    environmentConfig: EnvironmentConfig,
    private val identity: UserIdentity,
    offlineAccounts: OfflineAccountUpdater,
    private val dotFeatureFlag: FeatureFlag
) : CryptoAssetBase(
    payloadManager,
    exchangeRates,
    historicRates,
    currencyPrefs,
    labels,
    custodialManager,
    pitLinking,
    crashLogger,
    environmentConfig,
    offlineAccounts,
    identity
) {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.DOT

    private val isDotFeatureFlagEnabled = AtomicBoolean(false)

    override val isEnabled: Boolean
        get() = isDotFeatureFlagEnabled.get()

    override fun initToken(): Completable {
        return dotFeatureFlag.enabled.doOnSuccess {
            isDotFeatureFlagEnabled.set(it)
        }.flatMapCompletable {
            Completable.complete()
        }
    }

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.just(emptyList())

    override fun loadCustodialAccount(): Single<SingleAccountList> =
        Single.just(
            listOf(
                PolkadotCustodialTradingAccount(
                    asset,
                    labels.getDefaultCustodialWalletLabel(asset),
                    exchangeRates,
                    custodialManager,
                    environmentConfig,
                    identity
                )
            )
        )

    override fun parseAddress(address: String): Maybe<ReceiveAddress> = Maybe.empty()
}

internal class PolkadotAddress(
    override val address: String,
    override val label: String = address
) : CryptoAddress {
    override val asset: CryptoCurrency = CryptoCurrency.DOT
}