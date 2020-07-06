package piuk.blockchain.android.coincore.pax

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.erc20.FeedErc20Transfer
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.utils.extensions.mapList
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicBoolean

internal class PaxCryptoWalletAccount(
    override val label: String,
    private val address: String,
    private val paxAccount: Erc20Account,
    override val exchangeRates: ExchangeRateDataManager
) : CryptoNonCustodialAccount(CryptoCurrency.PAX) {
    override val isDefault: Boolean = true // Only one account, so always default

    private var hasFunds = AtomicBoolean(false)

    override val isFunded: Boolean
        get() = hasFunds.get()

    override val balance: Single<CryptoValue>
        get() = paxAccount.getBalance()
            .map { CryptoValue.fromMinor(CryptoCurrency.PAX, it) }
            .doOnSuccess {
                if (it.amount > BigInteger.ZERO) {
                    hasFunds.set(true)
                }
            }

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(
            PaxAddress(address, label)
        )

    override val activity: Single<ActivitySummaryList>
        get() {
            val ethDataManager = paxAccount.ethDataManager

            val feedTransactions =
                paxAccount.fetchErc20Address()
                    .flatMap { paxAccount.getTransactions() }
                    .mapList {
                        val feeObservable = ethDataManager
                            .getTransaction(it.transactionHash)
                            .map { transaction ->
                                transaction.gasUsed * transaction.gasPrice
                            }
                        FeedErc20Transfer(it, feeObservable)
                    }

            return Singles.zip(
                feedTransactions,
                paxAccount.getAccountHash(),
                ethDataManager.getLatestBlockNumber()
            ).map { (transactions, accountHash, latestBlockNumber) ->
                transactions.map { transaction ->
                    PaxActivitySummaryItem(
                        feedTransfer = transaction,
                        accountHash = accountHash,
                        ethDataManager = ethDataManager,
                        exchangeRates = exchangeRates,
                        lastBlockNumber = latestBlockNumber.number,
                        account = this
                    ) as ActivitySummaryItem
                }
            }
            .doOnSuccess { setHasTransactions(it.isNotEmpty()) }
        }
}
