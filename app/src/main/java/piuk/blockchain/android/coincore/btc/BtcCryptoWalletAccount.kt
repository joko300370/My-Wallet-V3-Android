package piuk.blockchain.android.coincore.btc

import com.blockchain.serialization.JsonSerializableAccount
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.Single
import org.bitcoinj.core.NetworkParameters
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.android.coincore.impl.transactionFetchCount
import piuk.blockchain.android.coincore.impl.transactionFetchOffset
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.extensions.mapList
import java.util.concurrent.atomic.AtomicBoolean

internal class BtcCryptoWalletAccount(
    payloadManager: PayloadDataManager,
    override val label: String,
    private val address: String,
    override val isDefault: Boolean = false,
    override val exchangeRates: ExchangeRateDataManager,
    private val networkParameters: NetworkParameters,
    // TEMP keep a copy of the metadata account, for interop with the old send flow
    // this can and will be removed when BTC is moved over and has a on-chain
    // TransactionProcessor defined;
    val internalAccount: JsonSerializableAccount
) : CryptoNonCustodialAccount(payloadManager, CryptoCurrency.BTC) {

    private val hasFunds = AtomicBoolean(false)

    override val isFunded: Boolean
        get() = hasFunds.get()

    override val accountBalance: Single<Money>
        get() = payloadManager.getAddressBalanceRefresh(address)
            .doOnSuccess {
                hasFunds.set(it > CryptoValue.ZeroBtc)
            }
            .map { it as Money }

    override val actionableBalance: Single<Money>
        get() = accountBalance

    override val receiveAddress: Single<ReceiveAddress>
        get() = payloadManager.getNextReceiveAddress(
            // TODO: Probably want the index of this address'
            payloadManager.getAccount(payloadManager.defaultAccountIndex)
        ).singleOrError()
            .map {
                BtcAddress(address = it, label = label, networkParams = networkParameters)
            }

    override val activity: Single<ActivitySummaryList>
        get() = payloadManager.getAccountTransactions(
            address,
            transactionFetchCount,
            transactionFetchOffset
        )
        .onErrorReturn { emptyList() }
        .mapList {
            BtcActivitySummaryItem(
                it,
                payloadManager,
                exchangeRates,
                this
            ) as ActivitySummaryItem
        }.doOnSuccess {
            setHasTransactions(it.isNotEmpty())
        }

    override fun createTxEngine(): TxEngine {
        TODO("Not yet implemented")
    }

    constructor(
        jsonAccount: Account,
        payloadManager: PayloadDataManager,
        isDefault: Boolean = false,
        exchangeRates: ExchangeRateDataManager,
        networkParameters: NetworkParameters
    ) : this(
        payloadManager,
        jsonAccount.label,
        jsonAccount.xpub,
        isDefault,
        exchangeRates,
        networkParameters,
        jsonAccount
    )

    constructor(
        legacyAccount: LegacyAddress,
        payloadManager: PayloadDataManager,
        exchangeRates: ExchangeRateDataManager,
        networkParameters: NetworkParameters
    ) : this(
        payloadManager,
        legacyAccount.label ?: legacyAccount.address,
        legacyAccount.address,
        false,
        exchangeRates,
        networkParameters,
        legacyAccount
    )
}