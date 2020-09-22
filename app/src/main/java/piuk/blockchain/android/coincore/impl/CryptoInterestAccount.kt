package piuk.blockchain.android.coincore.impl

import com.blockchain.extensions.exhaustive
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.InterestActivityItem
import com.blockchain.swap.nabu.datamanagers.InterestState
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.CustodialInterestActivitySummaryItem
import piuk.blockchain.android.coincore.InterestAccount
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.TxSourceState
import piuk.blockchain.android.coincore.bch.BchAddress
import piuk.blockchain.android.coincore.btc.BtcAddress
import piuk.blockchain.android.coincore.erc20.Erc20Address
import piuk.blockchain.android.coincore.eth.EthAddress
import piuk.blockchain.android.coincore.xlm.XlmAddress
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.utils.extensions.mapList
import java.util.concurrent.atomic.AtomicBoolean

internal class CryptoInterestAccount(
    override val asset: CryptoCurrency,
    override val label: String,
    val custodialWalletManager: CustodialWalletManager,
    override val exchangeRates: ExchangeRateDataManager,
    private val environmentConfig: EnvironmentConfig
) : CryptoAccountBase(), InterestAccount {

    private val nabuAccountExists = AtomicBoolean(false)
    private val hasFunds = AtomicBoolean(false)

    override val receiveAddress: Single<ReceiveAddress>
        get() = custodialWalletManager.getInterestAccountAddress(asset).map {
            when (asset) {
                CryptoCurrency.PAX,
                CryptoCurrency.USDT -> {
                    Erc20Address(
                        asset = asset,
                        address = it,
                        label = label
                    )
                }
                CryptoCurrency.ETHER -> {
                    EthAddress(
                        address = it,
                        label = label
                    )
                }
                CryptoCurrency.BTC -> {
                    BtcAddress(
                        address = it,
                        label = label,
                        networkParams = environmentConfig.bitcoinNetworkParameters
                    )
                }
                CryptoCurrency.BCH -> {
                    BchAddress(
                        address_ = it,
                        label = label,
                        scanUri = null
                    )
                }
                CryptoCurrency.XLM -> {
                    XlmAddress(
                        address = it,
                        label = label
                    )
                }
                CryptoCurrency.ALGO,
                CryptoCurrency.STX -> throw IllegalStateException(
                    "Interest receive address not supported for asset: $asset")
            }.exhaustive
        }

    override fun requireSecondPassword(): Single<Boolean> =
        Single.just(false)

    override val accountBalance: Single<Money>
        get() = custodialWalletManager.getInterestAccountBalance(asset)
            .switchIfEmpty(
                Single.just(CryptoValue.zero(asset))
            )
            .doOnSuccess { hasFunds.set(it.isPositive) }
            .map { it as Money }

    override val actionableBalance: Single<Money>
        get() = accountBalance // TODO This will need updating when we support transfer out of an interest account

    override val activity: Single<ActivitySummaryList>
        get() = custodialWalletManager.getInterestActivity(asset)
            .mapList { interestActivityToSummary(it) }
            .filterActivityStates()
            .doOnSuccess {
                setHasTransactions(it.isNotEmpty())
            }
            .onErrorReturn { emptyList() }

    private fun interestActivityToSummary(item: InterestActivityItem): ActivitySummaryItem =
        CustodialInterestActivitySummaryItem(
            exchangeRates = exchangeRates,
            cryptoCurrency = item.cryptoCurrency,
            txId = item.id,
            timeStampMs = item.insertedAt.time,
            value = item.value,
            account = this,
            status = item.state,
            type = item.type,
            confirmations = item.extraAttributes?.confirmations ?: 0,
            accountRef = item.extraAttributes?.beneficiary?.accountRef ?: "",
            recipientAddress = item.extraAttributes?.address ?: ""
        )

    private fun Single<ActivitySummaryList>.filterActivityStates(): Single<ActivitySummaryList> {
        return flattenAsObservable { list ->
            list.filter {
                it is CustodialInterestActivitySummaryItem && displayedStates.contains(it.status)
            }
        }.toList()
    }

    fun isInterestEnabled() =
        custodialWalletManager.getInterestEnabledForAsset(asset)
            .map {
                nabuAccountExists.set(it)
            }

    val isConfigured: Boolean
        get() = nabuAccountExists.get()

    override val isFunded: Boolean
        get() = hasFunds.get()

    override val isDefault: Boolean =
        false // Default is, presently, only ever a non-custodial account.

    // TODO: Where is this called?
    override val sourceState: Single<TxSourceState>
        get() = Single.just(
            if (nabuAccountExists.get()) {
                TxSourceState.CAN_TRANSACT
            } else {
                TxSourceState.NOT_SUPPORTED
            }
        )
    override val actions: AvailableActions =
        if (asset.hasFeature(CryptoCurrency.IS_ERC20) || asset == CryptoCurrency.ETHER) {
            setOf(AssetAction.Deposit, AssetAction.Summary, AssetAction.ViewActivity)
        } else {
            setOf(AssetAction.Summary, AssetAction.ViewActivity)
        }

    companion object {
        private val displayedStates = setOf(
            InterestState.COMPLETE,
            InterestState.PROCESSING,
            InterestState.PENDING,
            InterestState.MANUAL_REVIEW
        )
    }
}