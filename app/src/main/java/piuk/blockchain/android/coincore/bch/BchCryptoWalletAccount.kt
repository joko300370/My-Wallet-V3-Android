package piuk.blockchain.android.coincore.bch

import com.blockchain.preferences.WalletStatus
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
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

internal class BchCryptoWalletAccount private constructor(
    payloadManager: PayloadDataManager,
    override val label: String,
    private val address: String,
    private val bchManager: BchDataManager,
    // Used to lookup the account in payloadDataManager to fetch receive address
    private val addressIndex: Int,
    override val isDefault: Boolean = false,
    override val exchangeRates: ExchangeRateDataManager,
    private val networkParams: NetworkParameters,
    private val feeDataManager: FeeDataManager,
    private val sendDataManager: SendDataManager,
    // TEMP keep a copy of the metadata account, for interop with the old send flow
    // this can and will be removed when BCH is moved over and has a on-chain
    // TransactionProcessor defined;
    val internalAccount: GenericMetadataAccount,
    private val walletPreferences: WalletStatus,
    private val custodialWalletManager: CustodialWalletManager,
    override val isArchived: Boolean
) : CryptoNonCustodialAccount(payloadManager, CryptoCurrency.BCH) {

    private val hasFunds = AtomicBoolean(false)

    override val isFunded: Boolean
        get() = hasFunds.get()

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
            addressIndex
        ).map {
            val address = Address.fromBase58(networkParams, it)
            address.toCashAddress()
        }.singleOrError()
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
            }
            .flatMap {
                appendSwapActivity(custodialWalletManager, asset, it)
            }
            .doOnSuccess { setHasTransactions(it.isNotEmpty()) }

    override fun createTxEngine(): TxEngine =
        BchOnChainTxEngine(
            feeDataManager = feeDataManager,
            networkParams = networkParams,
            sendDataManager = sendDataManager,
            bchDataManager = bchManager,
            payloadDataManager = payloadDataManager,
            requireSecondPassword = payloadDataManager.isDoubleEncrypted,
            walletPreferences = walletPreferences
        )

    companion object {
        fun createBchAccount(
            payloadManager: PayloadDataManager,
            jsonAccount: GenericMetadataAccount,
            bchManager: BchDataManager,
            addressIndex: Int,
            isDefault: Boolean,
            exchangeRates: ExchangeRateDataManager,
            networkParams: NetworkParameters,
            feeDataManager: FeeDataManager,
            sendDataManager: SendDataManager,
            walletPreferences: WalletStatus,
            custodialWalletManager: CustodialWalletManager,
            isArchived: Boolean
        ) = BchCryptoWalletAccount(
            payloadManager = payloadManager,
            label = jsonAccount.label,
            address = jsonAccount.xpub,
            bchManager = bchManager,
            addressIndex = addressIndex,
            isDefault = isDefault,
            exchangeRates = exchangeRates,
            networkParams = networkParams,
            feeDataManager = feeDataManager,
            sendDataManager = sendDataManager,
            internalAccount = jsonAccount,
            walletPreferences = walletPreferences,
            custodialWalletManager = custodialWalletManager,
            isArchived = isArchived
        )

        @Deprecated("Used in legacy Account List to generate a transfer source when importing addresses")
        fun createImportBchAccount(
            payloadManager: PayloadDataManager,
            label: String,
            address: String,
            jsonAccount: GenericMetadataAccount,
            bchManager: BchDataManager,
            exchangeRates: ExchangeRateDataManager,
            networkParams: NetworkParameters,
            feeDataManager: FeeDataManager,
            sendDataManager: SendDataManager,
            walletPreferences: WalletStatus,
            custodialWalletManager: CustodialWalletManager,
            isArchived: Boolean
        ) = BchCryptoWalletAccount(
            payloadManager = payloadManager,
            label = label,
            address = address,
            bchManager = bchManager,
            addressIndex = IMPORT_ADDRESS_NO_INDEX,
            isDefault = false,
            exchangeRates = exchangeRates,
            networkParams = networkParams,
            feeDataManager = feeDataManager,
            sendDataManager = sendDataManager,
            internalAccount = jsonAccount,
            walletPreferences = walletPreferences,
            custodialWalletManager = custodialWalletManager,
            isArchived = isArchived
        )

        private const val IMPORT_ADDRESS_NO_INDEX = -1
    }
}
