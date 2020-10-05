package piuk.blockchain.android.coincore.btc

import com.blockchain.preferences.WalletStatus
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
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager
import piuk.blockchain.androidcore.utils.extensions.mapList
import java.util.concurrent.atomic.AtomicBoolean

internal class BtcCryptoWalletAccount(
    payloadManager: PayloadDataManager,
    private val sendDataManager: SendDataManager,
    private val feeDataManager: FeeDataManager,
    override val label: String,
    private val address: String,
    override val isDefault: Boolean = false,
    override val exchangeRates: ExchangeRateDataManager,
    private val networkParameters: NetworkParameters,
    // TEMP keep a copy of the metadata account, for interop with the old send flow
    // this can and will be removed when BTC is moved over and has a on-chain
    // TransactionProcessor defined;
    @Deprecated("Old send style address format")
    val internalAccount: JsonSerializableAccount,
    val isHDAccount: Boolean,
    private val walletPreferences: WalletStatus
) : CryptoNonCustodialAccount(payloadManager, CryptoCurrency.BTC) {

    private val hasFunds = AtomicBoolean(false)

    override val isFunded: Boolean
        get() = hasFunds.get()

    override val accountBalance: Single<Money>
        get() = payloadDataManager.getAddressBalanceRefresh(address)
            .doOnSuccess {
                hasFunds.set(it > CryptoValue.ZeroBtc)
            }
            .map { it as Money }

    override val actionableBalance: Single<Money>
        get() = accountBalance

    override val receiveAddress: Single<ReceiveAddress>
        get() = payloadDataManager.getNextReceiveAddress(
            // TODO: Probably want the index of this address'
            payloadDataManager.getAccount(payloadDataManager.defaultAccountIndex)
        ).singleOrError()
            .map {
                BtcAddress(address = it, label = label, networkParams = networkParameters)
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

    override fun createTxEngine(): TxEngine =
        BtcOnChainTxEngine(
            btcDataManager = payloadDataManager,
            sendDataManager = sendDataManager,
            feeDataManager = feeDataManager,
            btcNetworkParams = networkParameters,
            requireSecondPassword = payloadDataManager.isDoubleEncrypted,
            walletPreferences = walletPreferences
        )

    companion object {
        fun createHdAccount(
            jsonAccount: Account,
            payloadManager: PayloadDataManager,
            sendDataManager: SendDataManager,
            feeDataManager: FeeDataManager,
            isDefault: Boolean = false,
            exchangeRates: ExchangeRateDataManager,
            networkParameters: NetworkParameters,
            walletPreferences: WalletStatus
        ) = BtcCryptoWalletAccount(
            payloadManager = payloadManager,
            sendDataManager = sendDataManager,
            feeDataManager = feeDataManager,
            label = jsonAccount.label,
            address = jsonAccount.xpub,
            isDefault = isDefault,
            exchangeRates = exchangeRates,
            networkParameters = networkParameters,
            internalAccount = jsonAccount,
            isHDAccount = true,
            walletPreferences = walletPreferences
        )

        fun createLegacyAccount(
            legacyAccount: LegacyAddress,
            payloadManager: PayloadDataManager,
            sendDataManager: SendDataManager,
            feeDataManager: FeeDataManager,
            exchangeRates: ExchangeRateDataManager,
            networkParameters: NetworkParameters,
            walletPreferences: WalletStatus
        ) = BtcCryptoWalletAccount(
            payloadManager = payloadManager,
            sendDataManager = sendDataManager,
            feeDataManager = feeDataManager,
            label = legacyAccount.label ?: legacyAccount.address,
            address = legacyAccount.address,
            isDefault = false,
            exchangeRates = exchangeRates,
            networkParameters = networkParameters,
            internalAccount = legacyAccount,
            isHDAccount = false,
            walletPreferences = walletPreferences

        )
    }
}