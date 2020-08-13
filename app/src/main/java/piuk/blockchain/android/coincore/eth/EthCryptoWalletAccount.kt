package piuk.blockchain.android.coincore.eth

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.wallet.ethereum.EthereumAccount
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.ENABLE_NEW_SEND_ACTION
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SendState
import piuk.blockchain.android.coincore.SendTarget
import piuk.blockchain.android.coincore.TransactionProcessor
import piuk.blockchain.android.coincore.TransferError
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import java.util.concurrent.atomic.AtomicBoolean

internal class EthCryptoWalletAccount(
    payloadManager: PayloadDataManager,
    override val label: String,
    internal val address: String,
    private val ethDataManager: EthDataManager,
    private val fees: FeeDataManager,
    override val exchangeRates: ExchangeRateDataManager
) : CryptoNonCustodialAccount(payloadManager, CryptoCurrency.ETHER) {

    constructor(
        payloadManager: PayloadDataManager,
        ethDataManager: EthDataManager,
        fees: FeeDataManager,
        jsonAccount: EthereumAccount,
        exchangeRates: ExchangeRateDataManager
    ) : this(
        payloadManager,
        jsonAccount.label,
        jsonAccount.address,
        ethDataManager,
        fees,
        exchangeRates
    )

    private val hasFunds = AtomicBoolean(false)

    override val isFunded: Boolean
        get() = hasFunds.get()

    override val balance: Single<Money>
        get() = ethDataManager.fetchEthAddress()
            .singleOrError()
            .map { CryptoValue(CryptoCurrency.ETHER, it.getTotalBalance()) }
            .doOnSuccess {
                hasFunds.set(it > CryptoValue.ZeroEth)
            }
            .map { it as Money }

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
                            EthActivitySummaryItem(
                                ethDataManager,
                                transaction,
                                isErc20FeeTransaction(transaction.to),
                                latestBlock.number.toLong(),
                                exchangeRates,
                                account = this
                            ) as ActivitySummaryItem
                        }
                    }
            }
            .doOnSuccess { setHasTransactions(it.isNotEmpty()) }

    private fun isErc20FeeTransaction(to: String): Boolean =
        CryptoCurrency.erc20Assets().firstOrNull {
            to.equals(ethDataManager.getErc20TokenData(it).contractAddress, true)
        } != null

    override val isDefault: Boolean = true // Only one ETH account, so always default

    override fun createSendProcessor(sendTo: SendTarget): Single<TransactionProcessor> =
        when (sendTo) {
            is CryptoAddress -> Single.just(
                EthSendTransaction(
                    ethDataManager = ethDataManager,
                    feeManager = fees,
                    exchangeRates = exchangeRates,
                    sendingAccount = this,
                    sendTarget = sendTo,
                    requireSecondPassword = ethDataManager.requireSecondPassword
                )
            )
            is CryptoAccount -> sendTo.receiveAddress.map {
                EthSendTransaction(
                    ethDataManager = ethDataManager,
                    feeManager = fees,
                    exchangeRates = exchangeRates,
                    sendingAccount = this,
                    sendTarget = it as CryptoAddress,
                    requireSecondPassword = ethDataManager.requireSecondPassword
                )
            }
            else -> Single.error(TransferError("Cannot send custodial crypto to a non-crypto target"))
        }

    override val sendState: Single<SendState>
        get() = Singles.zip(
                balance,
                ethDataManager.isLastTxPending()
            ) { balance: Money, hasUnconfirmed: Boolean ->
                when {
                    balance.isZero -> SendState.NO_FUNDS
                    hasUnconfirmed -> SendState.SEND_IN_FLIGHT
                    else -> SendState.CAN_SEND
                }
            }

    override val actions: AvailableActions
        get() = super.actions.let {
            if (it.contains(AssetAction.Send)) {
                it.toMutableSet().apply {
                    if (ENABLE_NEW_SEND_ACTION) {
                        remove(AssetAction.Send)
                        add(AssetAction.NewSend)
                    }
                }
            } else {
                it
            }
        }
}
