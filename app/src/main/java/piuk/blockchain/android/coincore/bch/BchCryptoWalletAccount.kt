package piuk.blockchain.android.coincore.bch

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.wallet.coin.GenericMetadataAccount
import io.reactivex.Single
import org.bitcoinj.core.Address
import org.bitcoinj.core.NetworkParameters
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.btc.BtcAddress
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.android.coincore.impl.transactionFetchCount
import piuk.blockchain.android.coincore.impl.transactionFetchOffset
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.utils.extensions.mapList
import java.util.concurrent.atomic.AtomicBoolean

internal class BchCryptoWalletAccount(
    override val label: String,
    private val address: String,
    private val bchManager: BchDataManager,
    override val isDefault: Boolean = false,
    override val exchangeRates: ExchangeRateDataManager,
    private val networkParams: NetworkParameters,
    override val feeAsset: CryptoCurrency? = CryptoCurrency.BCH
) : CryptoNonCustodialAccount(CryptoCurrency.BCH) {

    private val hasFunds = AtomicBoolean(false)

    override val isFunded: Boolean
        get() = hasFunds.get()

    override val balance: Single<Money>
        get() = bchManager.getBalance(address)
            .map { CryptoValue.fromMinor(CryptoCurrency.BCH, it) }
            .doOnSuccess {
                hasFunds.set(it > CryptoValue.ZeroBch)
            }
            .map { it as Money }

    override val receiveAddress: Single<ReceiveAddress>
        get() = bchManager.getNextReceiveAddress(
            bchManager.getAccountMetadataList()
                .indexOfFirst {
                    it.xpub == bchManager.getDefaultGenericMetadataAccount()!!.xpub
                }
            ).map {
                val address = Address.fromBase58(networkParams, it)
                address.toCashAddress()
            }
            .singleOrError()
            .map {
                BtcAddress(it, label)
            }

    override val activity: Single<ActivitySummaryList>
        get() = bchManager.getAddressTransactions(address, transactionFetchCount, transactionFetchOffset)
            .onErrorReturn { emptyList() }
            .mapList {
                BchActivitySummaryItem(
                    it,
                    exchangeRates,
                    account = this
                ) as ActivitySummaryItem
            }.doOnSuccess { setHasTransactions(it.isNotEmpty()) }

    constructor(
        jsonAccount: GenericMetadataAccount,
        bchManager: BchDataManager,
        isDefault: Boolean,
        exchangeRates: ExchangeRateDataManager,
        networkParams: NetworkParameters
    ) : this(
        jsonAccount.label,
        jsonAccount.xpub,
        bchManager,
        isDefault,
        exchangeRates,
        networkParams
    )
}
