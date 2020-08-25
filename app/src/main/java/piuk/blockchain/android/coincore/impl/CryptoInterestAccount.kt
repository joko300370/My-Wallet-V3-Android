package piuk.blockchain.android.coincore.impl

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
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.CustodialInterestActivitySummaryItem
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SendState
import piuk.blockchain.android.coincore.SendTarget
import piuk.blockchain.android.coincore.TransactionProcessor
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.utils.extensions.mapList
import java.util.concurrent.atomic.AtomicBoolean

internal class CryptoInterestAccount(
    override val asset: CryptoCurrency,
    override val label: String,
    val custodialWalletManager: CustodialWalletManager,
    override val exchangeRates: ExchangeRateDataManager
) : CryptoAccountBase() {

    private val nabuAccountExists = AtomicBoolean(false)
    private val hasFunds = AtomicBoolean(false)

    override val receiveAddress: Single<ReceiveAddress>
        get() = custodialWalletManager.getInterestAccountAddress(asset).map {
            InterestAddress(
                address = it,
                label = label,
                asset = asset
            )
        }

    override fun requireSecondPassword(): Single<Boolean> =
        Single.just(false)

    override val balance: Single<Money>
        get() = custodialWalletManager.getInterestAccountDetails(asset)
            .switchIfEmpty(
                Single.just(CryptoValue.zero(asset))
            )
            .doOnSuccess { hasFunds.set(it.isPositive) }
            .map { it as Money }

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

    override fun createSendProcessor(sendTo: SendTarget): Single<TransactionProcessor> =
        Single.error<TransactionProcessor>(NotImplementedError("Cannot Send from Interest Wallet"))

    override val sendState: Single<SendState>
        get() = Single.just(SendState.NOT_SUPPORTED)

    override val actions: AvailableActions =
        if (asset.hasFeature(CryptoCurrency.IS_ERC20) || asset == CryptoCurrency.ETHER) {
            // TODO coming soon - AssetAction.Summary
            setOf(AssetAction.Deposit, AssetAction.ViewActivity)
        } else {
            setOf(AssetAction.ViewActivity)
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

internal class InterestAddress(
    override val address: String,
    override val label: String = address,
    override val asset: CryptoCurrency
) : CryptoAddress
