package piuk.blockchain.android.coincore.xlm

import com.blockchain.sunriver.XlmDataManager
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.TransactionProcessor
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.extensions.mapList
import java.util.concurrent.atomic.AtomicBoolean

internal class XlmCryptoWalletAccount(
    payloadManager: PayloadDataManager,
    override val label: String = "",
    private val address: String,
    private val xlmManager: XlmDataManager,
    override val exchangeRates: ExchangeRateDataManager
) : CryptoNonCustodialAccount(payloadManager, CryptoCurrency.XLM) {

    override val isDefault: Boolean = true // Only one account ever, so always default

    private val hasFunds = AtomicBoolean(false)

    override val isFunded: Boolean
        get() = hasFunds.get()

    override val accountBalance: Single<Money>
        get() = xlmManager.getBalance()
            .doOnSuccess {
                hasFunds.set(it > CryptoValue.ZeroXlm)
            }
            .map { it as Money }

    override val actionableBalance: Single<Money>
        get() = accountBalance

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(
            XlmAddress(address = address, label = label)
        )

    override val activity: Single<ActivitySummaryList>
        get() = xlmManager.getTransactionList()
            .mapList {
                XlmActivitySummaryItem(
                    it,
                    exchangeRates,
                    account = this
                ) as ActivitySummaryItem
            }
            .doOnSuccess { setHasTransactions(it.isNotEmpty()) }

    override fun createTransactionProcessor(target: TransactionTarget): Single<TransactionProcessor> {
        TODO("Implement me")
    }

    constructor(
        payloadManager: PayloadDataManager,
        account: AccountReference.Xlm,
        xlmManager: XlmDataManager,
        exchangeRates: ExchangeRateDataManager
    ) : this(
        payloadManager = payloadManager,
        label = account.label,
        address = account.accountId,
        xlmManager = xlmManager,
        exchangeRates = exchangeRates
    )
}
