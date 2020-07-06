package piuk.blockchain.android.coincore.eth

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.ethereum.EthereumAccount
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SendProcessor
import piuk.blockchain.android.coincore.SendState
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicBoolean

internal class EthCryptoWalletAccount(
    override val label: String,
    internal val address: String,
    private val ethDataManager: EthDataManager,
    private val fees: FeeDataManager,
    override val exchangeRates: ExchangeRateDataManager
) : CryptoNonCustodialAccount(CryptoCurrency.ETHER) {

    constructor(
        ethDataManager: EthDataManager,
        fees: FeeDataManager,
        jsonAccount: EthereumAccount,
        exchangeRates: ExchangeRateDataManager
    ) : this(
        jsonAccount.label,
        jsonAccount.address,
        ethDataManager,
        fees,
        exchangeRates
    )

    private var hasFunds = AtomicBoolean(false)

    override val isFunded: Boolean
        get() = hasFunds.get()

    override val balance: Single<CryptoValue>
        get() = ethDataManager.fetchEthAddress()
            .singleOrError()
            .map { CryptoValue(CryptoCurrency.ETHER, it.getTotalBalance()) }
            .doOnSuccess {
                if (it.amount > BigInteger.ZERO) {
                    hasFunds.set(true)
                }
            }

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(
            EthAddress(
                    address = address,
                    label = label
            )
        )

    override val activity: Single<ActivitySummaryList>
        get() = ethDataManager.getLatestBlockNumber()
            .flatMap { latestBlock ->
                ethDataManager.getEthTransactions()
                    .map { list ->
                        list.map { transaction ->
                            val ethFeeForPaxTransaction = transaction.to.equals(
                                ethDataManager.getErc20TokenData(CryptoCurrency.PAX).contractAddress,
                                ignoreCase = true
                            )
                            EthActivitySummaryItem(
                                ethDataManager,
                                transaction,
                                ethFeeForPaxTransaction,
                                latestBlock.number.toLong(),
                                exchangeRates,
                                account = this
                            ) as ActivitySummaryItem
                        }
                    }
            }
            .doOnSuccess { setHasTransactions(it.isNotEmpty()) }

    override val isDefault: Boolean = true // Only one ETH account, so always default

    override fun createSendProcessor(address: ReceiveAddress): Single<SendProcessor> =
        // Check type of Address here, and create Custodial or Swap or Sell or
        // however this is going to work.
        //
        // For now, while I prototype this, just make the eth -> on chain eth object
        Single.just(
            EthSendTransaction(
                ethDataManager,
                fees,
                this,
                address as CryptoAddress,
                ethDataManager.requireSecondPassword
            )
        )

    override val sendState: Single<SendState>
        get() = Singles.zip(
                balance,
                ethDataManager.isLastTxPending()
            ) { balance: CryptoValue, hasUnconfirmed: Boolean ->
                when {
                    balance.isZero -> SendState.NO_FUNDS
                    hasUnconfirmed -> SendState.SEND_IN_FLIGHT
                    else -> SendState.CAN_SEND
                }
            }
}
