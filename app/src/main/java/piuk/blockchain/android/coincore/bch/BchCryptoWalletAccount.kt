package piuk.blockchain.android.coincore.bch

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.coin.GenericMetadataAccount
import io.reactivex.Single
import org.bitcoinj.core.Address
import org.bitcoinj.core.NetworkParameters
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountNonCustodialBase
import piuk.blockchain.android.coincore.impl.transactionFetchCount
import piuk.blockchain.android.coincore.impl.transactionFetchOffset
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.utils.extensions.mapList

internal class BchCryptoWalletAccount(
    override val label: String,
    private val address: String,
    private val bchManager: BchDataManager,
    override val isDefault: Boolean = false,
    override val exchangeRates: ExchangeRateDataManager,
    private val networkParams: NetworkParameters
) : CryptoSingleAccountNonCustodialBase(CryptoCurrency.BCH) {

    override val balance: Single<CryptoValue>
        get() = bchManager.getBalance(address)
            .map { CryptoValue.fromMinor(CryptoCurrency.BCH, it) }

    override val receiveAddress: Single<String>
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
