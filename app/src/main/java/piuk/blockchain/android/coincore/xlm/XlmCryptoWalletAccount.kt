package piuk.blockchain.android.coincore.xlm

import com.blockchain.sunriver.XlmDataManager
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountNonCustodialBase
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.utils.extensions.mapList
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicBoolean

internal class XlmCryptoWalletAccount(
    override val label: String = "",
    private val address: String,
    private val xlmManager: XlmDataManager,
    override val exchangeRates: ExchangeRateDataManager
) : CryptoSingleAccountNonCustodialBase(CryptoCurrency.XLM) {

    override val isDefault: Boolean = true // Only one account ever, so always default

    private var hasFunds = AtomicBoolean(false)

    override val isFunded: Boolean
        get() = hasFunds.get()

    override val balance: Single<CryptoValue>
        get() = xlmManager.getBalance()
            .doOnSuccess {
            if (it.amount > BigInteger.ZERO) {
                hasFunds.set(true)
            }
        }

    override val receiveAddress: Single<String>
        get() = Single.just(address)

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

    constructor(
        account: AccountReference.Xlm,
        xlmManager: XlmDataManager,
        exchangeRates: ExchangeRateDataManager
    ) : this(account.label, account.accountId, xlmManager, exchangeRates)
}