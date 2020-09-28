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
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.android.coincore.impl.transactionFetchCount
import piuk.blockchain.android.coincore.impl.transactionFetchOffset
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager
import piuk.blockchain.androidcore.utils.extensions.mapList
import java.util.concurrent.atomic.AtomicBoolean

internal class BchCryptoWalletAccount(
    payloadManager: PayloadDataManager,
    override val label: String,
    private val address: String,
    private val bchManager: BchDataManager,
    override val isDefault: Boolean = false,
    override val exchangeRates: ExchangeRateDataManager,
    private val networkParams: NetworkParameters,
    private val feeDataManager: FeeDataManager,
    private val sendDataManager: SendDataManager,
    // TEMP keep a copy of the metadata account, for interop with the old send flow
    // this can and will be removed when BCH is moved over and has a on-chain
    // TransactionProcessor defined;
    val internalAccount: GenericMetadataAccount
) : CryptoNonCustodialAccount(payloadManager, CryptoCurrency.BCH) {

    private val hasFunds = AtomicBoolean(false)

    override val isFunded: Boolean
        get() = hasFunds.get()

    // From receive presenter:
//    compositeDisposable += bchDataManager.updateAllBalances()
//    .doOnSubscribe { view?.showQrLoading() }
//    .andThen(
//    bchDataManager.getWalletTransactions(50, 0)
//    .onErrorReturn { emptyList() }
//    )
//    .flatMap { bchDataManager.getNextReceiveAddress(position) }
// it may be that we need to update tx's etc to get a legitimat receive address

    override val accountBalance: Single<Money>
        get() = bchManager.getBalance(address)
            .map { CryptoValue.fromMinor(CryptoCurrency.BCH, it) }
            .doOnSuccess {
                hasFunds.set(it > CryptoValue.ZeroBch)
            }
            .map { it as Money }

    override val actionableBalance: Single<Money>
        get() = accountBalance

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
                BchAddress(address_ = it, label = label)
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

    override fun createTxEngine(): TxEngine =
        BchOnChainTxEngine(
            feeDataManager = feeDataManager,
            networkParams = networkParams,
            sendDataManager = sendDataManager,
            bchDataManager = bchManager,
            payloadDataManager = payloadDataManager,
            requireSecondPassword = payloadDataManager.isDoubleEncrypted
        )

    constructor(
        payloadManager: PayloadDataManager,
        jsonAccount: GenericMetadataAccount,
        bchManager: BchDataManager,
        isDefault: Boolean,
        exchangeRates: ExchangeRateDataManager,
        networkParams: NetworkParameters,
        feeDataManager: FeeDataManager,
        sendDataManager: SendDataManager
    ) : this(
        payloadManager,
        jsonAccount.label,
        jsonAccount.xpub,
        bchManager,
        isDefault,
        exchangeRates,
        networkParams,
        feeDataManager,
        sendDataManager,
        jsonAccount
    )
}
