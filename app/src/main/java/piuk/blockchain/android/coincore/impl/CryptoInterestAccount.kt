package piuk.blockchain.android.coincore.impl

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.ENABLE_INTEREST_ACTIONS
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SendProcessor
import piuk.blockchain.android.coincore.SendState
import piuk.blockchain.android.coincore.SendTarget
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import java.util.concurrent.atomic.AtomicBoolean

internal class CryptoInterestAccount(
    override val asset: CryptoCurrency,
    override val label: String,
    val custodialWalletManager: CustodialWalletManager,
    override val exchangeRates: ExchangeRateDataManager
) : CryptoAccountBase() {

    override val feeAsset: CryptoCurrency? = null

    private val isConfigured = AtomicBoolean(false)

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.error(NotImplementedError("Interest accounts don't support receive"))

    override val balance: Single<Money>
        get() = custodialWalletManager.getInterestAccountDetails(asset)
            .doOnSuccess {
                isConfigured.set(true)
            }.doOnComplete {
                isConfigured.set(false)
            }.switchIfEmpty(
                Single.just(CryptoValue.zero(asset))
            )
            .map { it as Money }

    override val activity: Single<ActivitySummaryList>
        get() = Single.just(emptyList())

    override val isFunded: Boolean
        get() = isConfigured.get()

    override val isDefault: Boolean =
        false // Default is, presently, only ever a non-custodial account.

    override fun createSendProcessor(sendTo: SendTarget): Single<SendProcessor> =
        Single.error<SendProcessor>(NotImplementedError("Cannot Send from Interest Wallet"))

    override val sendState: Single<SendState>
        get() = Single.just(SendState.NOT_SUPPORTED)

    override val actions: AvailableActions = if(ENABLE_INTEREST_ACTIONS) {
        setOf(AssetAction.Deposit, AssetAction.Summary, AssetAction.ViewActivity)
    } else {
        emptySet()
    }
}
