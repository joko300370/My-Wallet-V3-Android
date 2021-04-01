package piuk.blockchain.android.coincore.erc20

import com.blockchain.annotations.CommonCode
import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.EligibilityProvider
import com.blockchain.preferences.WalletStatus
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.android.coincore.AddressParseError
import piuk.blockchain.android.coincore.CachedAddress
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SimpleOfflineCacheItem
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.impl.CryptoAssetBase
import piuk.blockchain.android.coincore.impl.OfflineAccountUpdater
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateService
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import java.util.concurrent.atomic.AtomicBoolean

internal class Erc20Asset(
    override val asset: CryptoCurrency,
    private val featureFlag: FeatureFlag = EnabledErc20FeatureFlag(),
    payloadManager: PayloadDataManager,
    private val ethDataManager: EthDataManager,
    private val feeDataManager: FeeDataManager,
    private val walletPreferences: WalletStatus,
    custodialManager: CustodialWalletManager,
    exchangeRates: ExchangeRateDataManager,
    historicRates: ExchangeRateService,
    currencyPrefs: CurrencyPrefs,
    labels: DefaultLabels,
    pitLinking: PitLinking,
    crashLogger: CrashLogger,
    environmentConfig: EnvironmentConfig,
    eligibilityProvider: EligibilityProvider,
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
    environmentConfig,
    eligibilityProvider,
    offlineAccounts
) {

    private val isFeatureFlagEnabled = AtomicBoolean(false)

    override val isEnabled: Boolean
        get() = isFeatureFlagEnabled.get()

    override fun initToken(): Completable {
        return featureFlag.enabled.doOnSuccess {
            isFeatureFlagEnabled.set(it)
        }.flatMapCompletable {
            ethDataManager.fetchErc20DataModel(asset)
                .ignoreElements()
        }
    }

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.just(getNonCustodialAccount())
            .doOnSuccess { updateOfflineCache(it) }
            .map { listOf(it) }

    private fun getNonCustodialAccount(): Erc20NonCustodialAccount {
        val erc20Address = ethDataManager.getEthWallet()?.account?.address
            ?: throw Exception("No ${asset.networkTicker} wallet found")

        return Erc20NonCustodialAccount(
            payloadManager,
            asset,
            ethDataManager,
            erc20Address,
            feeDataManager,
            labels.getDefaultNonCustodialWalletLabel(asset),
            exchangeRates,
            walletPreferences,
            custodialManager
        )
    }

    private fun updateOfflineCache(account: Erc20NonCustodialAccount) {
        offlineAccounts.updateOfflineAddresses(
            Single.just(
                SimpleOfflineCacheItem(
                    networkTicker = asset.networkTicker,
                    accountLabel = account.label,
                    address = CachedAddress(
                        account.address,
                        account.address
                    )
                )
            )
        )
    }

    @CommonCode("Exists in EthAsset")
    override fun parseAddress(address: String): Maybe<ReceiveAddress> =
        Single.just(isValidAddress(address)).flatMapMaybe { isValid ->
            if (isValid) {
                ethDataManager.isContractAddress(address)
                    .flatMapMaybe { isContract ->
                        if (isContract) {
                            throw AddressParseError(AddressParseError.Error.ETH_UNEXPECTED_CONTRACT_ADDRESS)
                        } else {
                            Maybe.just(Erc20Address(asset, address))
                        }
                    }
            } else {
                Maybe.empty()
            }
        }

    override fun isValidAddress(address: String): Boolean =
        FormatsUtil.isValidEthereumAddress(address)
}
