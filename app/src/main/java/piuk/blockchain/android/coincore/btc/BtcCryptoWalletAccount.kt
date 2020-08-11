package piuk.blockchain.android.coincore.btc

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.ReceiveAddress
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
    override val exchangeRates: ExchangeRateDataManager
) : CryptoNonCustodialAccount(payloadManager, CryptoCurrency.BTC) {
    private val hasFunds = AtomicBoolean(false)

    override val isFunded: Boolean
        get() = hasFunds.get()

    override val balance: Single<Money>
        get() = payloadManager.getAddressBalanceRefresh(address)
            .doOnSuccess {
                hasFunds.set(it > CryptoValue.ZeroBtc)
            }
            .map { it as Money }

    override val receiveAddress: Single<ReceiveAddress>
        get() = payloadManager.getNextReceiveAddress(
            // TODO: Probably want the index of this address'
            payloadManager.getAccount(payloadManager.defaultAccountIndex)
        ).singleOrError()
            .map {
                BtcAddress(it, label)
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

    constructor(
        jsonAccount: Account,
        payloadManager: PayloadDataManager,
        isDefault: Boolean = false,
        exchangeRates: ExchangeRateDataManager
    ) : this(
        payloadManager,
        jsonAccount.label,
        jsonAccount.xpub,
        isDefault,
        exchangeRates
    )

    constructor(
        legacyAccount: LegacyAddress,
        payloadManager: PayloadDataManager,
        exchangeRates: ExchangeRateDataManager
    ) : this(
        payloadManager,
        legacyAccount.label ?: legacyAccount.address,
        legacyAccount.address,
        false,
        exchangeRates
    )
}