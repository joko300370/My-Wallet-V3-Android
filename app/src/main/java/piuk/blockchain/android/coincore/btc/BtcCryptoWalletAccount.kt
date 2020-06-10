package piuk.blockchain.android.coincore.btc

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountNonCustodialBase
import piuk.blockchain.android.coincore.impl.transactionFetchCount
import piuk.blockchain.android.coincore.impl.transactionFetchOffset
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.extensions.mapList
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicBoolean

internal class BtcCryptoWalletAccount(
    override val label: String,
    private val address: String,
    private val payloadDataManager: PayloadDataManager,
    override val isDefault: Boolean = false,
    override val exchangeRates: ExchangeRateDataManager
) : CryptoSingleAccountNonCustodialBase(CryptoCurrency.BTC) {
    private var hasFunds = AtomicBoolean(false)

    override val isFunded: Boolean
        get() = hasFunds.get()

    override val balance: Single<CryptoValue>
        get() = payloadDataManager.getAddressBalanceRefresh(address).doOnSuccess {
            if (it.amount > BigInteger.ZERO) {
                hasFunds.set(true)
            }
        }

    override val receiveAddress: Single<ReceiveAddress>
        get() = payloadDataManager.getNextReceiveAddress(
            // TODO: Probably want the index of this address'
            payloadDataManager.getAccount(payloadDataManager.defaultAccountIndex)
        ).singleOrError()
            .map {
                BtcAddress(it, label)
            }

    override val activity: Single<ActivitySummaryList>
        get() = payloadDataManager.getAccountTransactions(
            address,
            transactionFetchCount,
            transactionFetchOffset
        )
        .onErrorReturn { emptyList() }
        .mapList {
            BtcActivitySummaryItem(
                it,
                payloadDataManager,
                exchangeRates,
                this
            ) as ActivitySummaryItem
        }.doOnSuccess {
            setHasTransactions(it.isNotEmpty())
        }

    constructor(
        jsonAccount: Account,
        payloadDataManager: PayloadDataManager,
        isDefault: Boolean = false,
        exchangeRates: ExchangeRateDataManager
    ) : this(
        jsonAccount.label,
        jsonAccount.xpub,
        payloadDataManager,
        isDefault,
        exchangeRates
    )

    constructor(
        legacyAccount: LegacyAddress,
        payloadDataManager: PayloadDataManager,
        exchangeRates: ExchangeRateDataManager
    ) : this(
        legacyAccount.label ?: legacyAccount.address,
        legacyAccount.address,
        payloadDataManager,
        false,
        exchangeRates
    )
}