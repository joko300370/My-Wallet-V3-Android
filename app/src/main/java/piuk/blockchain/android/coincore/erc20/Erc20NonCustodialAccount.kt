package piuk.blockchain.android.coincore.erc20

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SendProcessor
import piuk.blockchain.android.coincore.SendState
import piuk.blockchain.android.coincore.eth.EthAddress
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.erc20.FeedErc20Transfer
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.utils.extensions.mapList

abstract class Erc20NonCustodialAccount(
    asset: CryptoCurrency,
    override val label: String,
    override val exchangeRates: ExchangeRateDataManager
) : CryptoNonCustodialAccount(asset) {
    abstract val erc20Account: Erc20Account

    private val ethDataManager: EthDataManager
        get() = erc20Account.ethDataManager

    override val isDefault: Boolean = true // Only one account, so always default

    override val balance: Single<Money>
        get() = erc20Account.getBalance()
            .map { CryptoValue.fromMinor(asset, it) }

    override val activity: Single<ActivitySummaryList>
        get() {
            val feedTransactions =
                erc20Account.fetchErc20Address()
                    .flatMap { erc20Account.getTransactions() }
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
                erc20Account.getAccountHash(),
                ethDataManager.getLatestBlockNumber()
            ) { transactions, accountHash, latestBlockNumber ->
                transactions.map { transaction ->
                    Erc20ActivitySummaryItem(
                        asset,
                        feedTransfer = transaction,
                        accountHash = accountHash,
                        ethDataManager = ethDataManager,
                        exchangeRates = exchangeRates,
                        lastBlockNumber = latestBlockNumber.number,
                        account = this
                    ) as ActivitySummaryItem
                }
            }.doOnSuccess { setHasTransactions(it.isNotEmpty()) }
        }

    override val feeAsset: CryptoCurrency? = CryptoCurrency.ETHER

    override val actions: AvailableActions = setOf(
        AssetAction.ViewActivity,
        AssetAction.Send, // TODO: NewSend
        AssetAction.Receive,
        AssetAction.Swap
    )

    override fun createSendProcessor(address: ReceiveAddress): Single<SendProcessor> =
        Single.just(
            Erc20SendTransaction(
                asset = asset,
                erc20Account = erc20Account,
//                fees,
                sendingAccount = this,
                address = address as EthAddress,
                requireSecondPassword = ethDataManager.requireSecondPassword
            )
        )

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
}